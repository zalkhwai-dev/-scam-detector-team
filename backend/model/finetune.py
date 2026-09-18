from datasets import Dataset, ClassLabel, disable_caching
from transformers import AutoTokenizer, AutoModelForSequenceClassification, TrainingArguments, Trainer
import evaluate
import numpy as np
import pandas as pd

# cache off, it keeps handing me the old labels
disable_caching()

# --- data ---

# SMS set, labels are already ham/spam/Smishing
sms_df = pd.read_csv("Dataset_5971.csv")
sms_df = sms_df[["TEXT", "LABEL"]].rename(columns={"TEXT": "text", "LABEL": "label"})
sms_df["label"] = sms_df["label"].str.strip()

# email phishing set
email_df = pd.read_csv("Phishing_Email.csv")
email_df = email_df[["Email Text", "Email Type"]].rename(columns={"Email Text": "text", "Email Type": "label"})
email_df = email_df.dropna(subset=["text", "label"])
# their words -> my words
email_label_map = {"Safe Email": "ham", "Phishing Email": "Smishing"}
email_df["label"] = email_df["label"].map(email_label_map)
# whatever didn't map is NaN now, drop it
email_df = email_df.dropna(subset=["label"])
# only 3k, otherwise the emails drown out the SMS
email_df = email_df.sample(n=min(len(email_df), 3000), random_state=42)

# phishing/benign jsonl
benign_df = pd.read_json("phishing_benign_email_dataset.jsonl", lines=True)
# subject and body are separate here, glue them
benign_df["text"] = benign_df["subject"] + ". " + benign_df["body"]
benign_df = benign_df[["text", "label"]]
benign_label_map = {"benign": "ham", "phishing": "Smishing"}
benign_df["label"] = benign_df["label"].map(benign_label_map)
benign_df = benign_df.dropna(subset=["label"])

# my fake-but-legit bank texts, so real alerts don't get flagged
# add more here whenever I find a false positive
bank_ham_df = pd.read_csv("legit_bank_alerts.csv")

# stack them all
combined_df = pd.concat([sms_df, email_df, benign_df, bank_ham_df], ignore_index=True)

# clean labels AFTER concat, the sources disagree on casing/spaces
combined_df["label"] = combined_df["label"].str.strip().str.lower()

# look at this before letting it train
print("Combined label counts:\n", combined_df["label"].value_counts())
print("Unique labels after normalization:", combined_df["label"].unique())

# --- hf dataset ---

# preserve_index=False or pandas sneaks in an extra column
dataset = Dataset.from_pandas(combined_df, preserve_index=False)

# class_encode_column is the one that actually turns strings into ints
# cast_column does NOT do it, stop reaching for it
dataset = dataset.class_encode_column("label")
label_feature = dataset.features["label"]
id2label = dict(enumerate(label_feature.names))
label2id = {name: i for i, name in enumerate(label_feature.names)}
# inference has to use this exact mapping
print("Label mapping:", label2id)

# split after encoding so all 3 splits share the same mapping
dataset = dataset.train_test_split(test_size=0.2, seed=42)
# cut that 20% in half -> 80/10/10
temp = dataset["test"].train_test_split(test_size=0.5, seed=42)
dataset["validation"] = temp["train"]
dataset["test"] = temp["test"]

# --- model ---

# starting from v2, saving as v3. check this path before every run
model_name = "./smishing-detector-v2/final"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForSequenceClassification.from_pretrained(
    model_name,
    num_labels=len(label2id),
    id2label=id2label,
    label2id=label2id,
    # old head might be a different size, this throws it away and re-inits
    ignore_mismatched_sizes=True,
)

# 128 tokens, fine for SMS, cuts the long emails short
def tokenize_fn(examples):
    return tokenizer(examples["text"], truncation=True, padding="max_length", max_length=128)

# no cache, same reason as up top
tokenized_dataset = dataset.map(tokenize_fn, batched=True, load_from_cache_file=False)

# --- metrics ---

accuracy = evaluate.load("accuracy")
f1 = evaluate.load("f1")

def compute_metrics(eval_pred):
    logits, labels = eval_pred
    preds = np.argmax(logits, axis=-1)
    # watch f1 not accuracy, classes are imbalanced
    return {
        "accuracy": accuracy.compute(predictions=preds, references=labels)["accuracy"],
        "f1": f1.compute(predictions=preds, references=labels, average="weighted")["f1"],
    }

# --- training ---

training_args = TrainingArguments(
    output_dir="./smishing-detector-v3",
    # these two have to match or load_best_model_at_end crashes
    eval_strategy="epoch",
    save_strategy="epoch",
    learning_rate=2e-5,
    # 8 is what fits in my VRAM
    per_device_train_batch_size=8,
    per_device_eval_batch_size=8,
    # 3 is enough, more and it memorizes
    num_train_epochs=3,
    weight_decay=0.01,
    load_best_model_at_end=True,
    # keep the best epoch, not the last one
    metric_for_best_model="f1",
    logging_steps=25,
)

trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=tokenized_dataset["train"],
    # validation here, test stays untouched
    eval_dataset=tokenized_dataset["validation"],
    compute_metrics=compute_metrics,
)

trainer.train()

# test runs once, at the end, don't tune against it
test_results = trainer.evaluate(tokenized_dataset["test"])
print("Test set results:", test_results)

# model + tokenizer in the same folder so loading is one path
trainer.save_model("./smishing-detector-v3/final")
tokenizer.save_pretrained("./smishing-detector-v3/final")

print("Training complete! Model saved to ./smishing-detector-v3/final")