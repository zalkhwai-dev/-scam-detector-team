from datasets import Dataset, ClassLabel, disable_caching
from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    DataCollatorWithPadding,
)
import evaluate
import numpy as np
import pandas as pd

disable_caching()

# --- data ---

sms_df = pd.read_csv("Dataset_5971.csv")
sms_df = sms_df[["TEXT", "LABEL"]].rename(columns={"TEXT": "text", "LABEL": "label"})
sms_df["label"] = sms_df["label"].str.strip()

email_df = pd.read_csv("Phishing_Email.csv")
email_df = email_df[["Email Text", "Email Type"]].rename(columns={"Email Text": "text", "Email Type": "label"})
email_df = email_df.dropna(subset=["text", "label"])
email_label_map = {"Safe Email": "ham", "Phishing Email": "Smishing"}
email_df["label"] = email_df["label"].map(email_label_map)
email_df = email_df.dropna(subset=["label"])
email_df = email_df.sample(n=min(len(email_df), 3000), random_state=42)

benign_df = pd.read_json("phishing_benign_email_dataset.jsonl", lines=True)
benign_df["text"] = benign_df["subject"] + ". " + benign_df["body"]
benign_df = benign_df[["text", "label"]]
benign_label_map = {"benign": "ham", "phishing": "Smishing"}
benign_df["label"] = benign_df["label"].map(benign_label_map)
benign_df = benign_df.dropna(subset=["label"])

bank_ham_df = pd.read_csv("legit_bank_alerts.csv")

combined_df = pd.concat([sms_df, email_df, benign_df, bank_ham_df], ignore_index=True)

combined_df = combined_df.dropna(subset=["text"])
combined_df["text"] = combined_df["text"].astype(str)
combined_df = combined_df[combined_df["text"].str.strip() != ""]

combined_df["label"] = combined_df["label"].str.strip().str.lower()

print("Combined label counts:\n", combined_df["label"].value_counts())
print("Unique labels after normalization:", combined_df["label"].unique())
# this will show ['ham' 'smishing' 'spam'] - 3 classes, kept as-is on purpose

# --- hf dataset ---

dataset = Dataset.from_pandas(combined_df, preserve_index=False)
dataset = dataset.class_encode_column("label")
label_feature = dataset.features["label"]
id2label = dict(enumerate(label_feature.names))
label2id = {name: i for i, name in enumerate(label_feature.names)}
print("Label mapping:", label2id)

dataset = dataset.train_test_split(test_size=0.2, seed=42)
temp = dataset["test"].train_test_split(test_size=0.5, seed=42)
dataset["validation"] = temp["train"]
dataset["test"] = temp["test"]

# --- model ---

model_name = "./smishing-detector-v3/final"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(
    model_name,
    num_labels=len(label2id),
    id2label=id2label,
    label2id=label2id,
    ignore_mismatched_sizes=True,
)

# check the real length distribution instead of guessing a number
sample_texts = combined_df["text"].sample(min(2000, len(combined_df)), random_state=42)
sample_lens = [len(tokenizer(t).input_ids) for t in sample_texts]
p50, p90, p95, p99 = np.percentile(sample_lens, [50, 90, 95, 99])
print(f"Token length percentiles -> 50th: {p50:.0f}  90th: {p90:.0f}  95th: {p95:.0f}  99th: {p99:.0f}  max: {max(sample_lens)}")

# pick MAX_LEN off the 95th percentile, rounded up to a clean number, capped at 512
MAX_LEN = int(min(512, max(128, np.ceil(p95 / 32) * 32)))
print(f"Using MAX_LEN = {MAX_LEN} (covers ~95% of the sample without padding everything to 512)")

def tokenize_fn(examples):
    return tokenizer(examples["text"], truncation=True, max_length=MAX_LEN)

tokenized_dataset = dataset.map(tokenize_fn, batched=True, load_from_cache_file=False)

data_collator = DataCollatorWithPadding(tokenizer=tokenizer)

# --- metrics ---

accuracy = evaluate.load("accuracy")
f1 = evaluate.load("f1")

def compute_metrics(eval_pred):
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=-1)
    return {
        "accuracy": accuracy.compute(predictions=preds, references=labels)["accuracy"],
        "f1": f1.compute(predictions=preds, references=labels, average="weighted")["f1"],
    }

# --- training ---

training_args = TrainingArguments(
    output_dir="./smishing-detector-v3l2",
    eval_strategy="epoch",
    save_strategy="epoch",
    learning_rate=1e-5,
    per_device_train_batch_size=8,   # T4 can handle full batch, no accumulation needed
    per_device_eval_batch_size=8,
    num_train_epochs=2,
    weight_decay=0.01,
    load_best_model_at_end=True,
    metric_for_best_model="f1",
    logging_steps=25,
    save_total_limit=2,
)

trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=tokenized_dataset["train"],
    eval_dataset=tokenized_dataset["validation"],
    compute_metrics=compute_metrics,
    data_collator=data_collator,
)

trainer.train()

test_results = trainer.evaluate(tokenized_dataset["test"])
print("Test set results:", test_results)

trainer.save_model("./smishing-detector-v3l2/final")
tokenizer.save_pretrained("./smishing-detector-v3l2/final")

print("Training complete! Model saved to ./smishing-detector-v3l2/final")