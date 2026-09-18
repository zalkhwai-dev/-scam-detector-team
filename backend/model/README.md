# smishing-detector

Classifier that flags text (SMS + emails) as ham / smishing / spam.
Two versions so far, notes on both below so I don't forget what changed and why.

## v3 - the original (max_length=128)

First real version. Trained fresh from v2, `max_length=128`.

128 tokens is fine for SMS (usually 15-40 tokens) but chops emails hard - only
keeps roughly the first 80-100 words, so anything past the opening lines of a
longer phishing email never gets seen by the model. Worked okay since SMS was
most of the data, but  longer emails were cut short so the  accuracy was probably worse than the test numbers
suggested because of this truncation.

## v3l2 - longer context (max_length up to 512)

Built on top of v3, not from scratch - loads `v3/final` as the starting checkpoint
and keeps training. Two changes from v3:

1. **max_length isn't hardcoded anymore.** The script actually measures token
   lengths across a sample of the real data first:
   ```
   50th percentile: 33 tokens   <- most of this is SMS, makes sense
   90th percentile: 411
   95th percentile: 759
   99th percentile: 2312
   max: 98430                   <- some huge outlier row, worth checking what that even is
   ```
   Then picks `max_length` off the 95th percentile, capped at 512 - which is the
   hard ceiling for this model architecture (position embeddings physically stop
   at 512, can't go higher without retraining the base model). Since the 95th
   percentile alone was already past 512, it landed on 512.

2. **Lower learning rate (1e-5 vs 2e-5) and fewer epochs (2 vs 3).** Since this
   is fine-tuning an already-fine-tuned model on the same rows again, wanted to
   avoid overwriting what v3 already learned or overfitting on repeat data.

Trained on Colab (T4 GPU).

## Labels

3 classes, not 2 - `ham`, `smishing`, `spam` (kept `spam` separate on purpose,
didn't fold it into smishing). Heads up: `spam` is a much smaller slice of the
data (489 examples vs 6712 ham / 1970 smishing), so it's the weakest of the
three classes just from having less to learn from.

```
{'ham': 0, 'smishing': 1, 'spam': 2}
```

Always double check this printed mapping matches before trusting predictions -
it's re-derived from the data each run, so if the data changes the ids could
shift.

## What it's trained on

- `Dataset_5971.csv` - original SMS set, ham/spam/Smishing
- `Phishing_Email.csv` - email phishing set, capped at 3k rows so it doesn't
  drown out the SMS data
- `phishing_benign_email_dataset.jsonl` - subject+body glued together
- `legit_bank_alerts.csv` - my own synthetic "real bank text" examples, added
  because genuine bank/delivery alerts kept getting flagged as smishing

## Model weights

Not in this repo, too big for git (~453MB, git caps at 100MB per file).
Grab them from the Releases page instead:

**[Download smishing-detector-v3l2.zip](../../releases/tag/v3l2)** (find the `v3l2` release, download the zip attached to it)

Unzip it, then point at the extracted `final` folder:

```python
from transformers import AutoModelForSequenceClassification, AutoTokenizer

model_path = "./smishing-detector-v3l2/final"  # wherever you unzipped it
model = AutoModelForSequenceClassification.from_pretrained(model_path)
tokenizer = AutoTokenizer.from_pretrained(model_path)
```

## Running inference

```python
classifier = pipeline(
    "text-classification",
    model=model_path,
    tokenizer=tokenizer,
    truncation=True,
    max_length=512,  # match whatever the model was trained at
)
```
