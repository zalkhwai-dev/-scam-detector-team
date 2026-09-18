from transformers import AutoTokenizer, pipeline

model_path = "./smishing-detector-v3l2/final"

tokenizer = AutoTokenizer.from_pretrained(model_path)

classifier = pipeline(
    "text-classification",
    model=model_path,
    tokenizer=tokenizer,
    truncation=True,
    max_length=512,
)

test_messages = [
    """"[Invitation to participate in the
Corizo X IBM Professional Upskilling Bootcamp]

Dear Students
Saudi Electronic University (SEU),


This is the final call for the registrations of
Corizo X IBM Professional Upskilling Bootcamp exclusively for all students of 
Saudi Electronic University

The curriculum aligns with international industry standards followed by organizations including Deloitte, Microsoft, Amazon, JP Morgan, Accenture, Barclays, HSBC, IBM, Oracle, SAP, Capgemini, EY, PwC,Cognizant, TCS, Infosys, Wipro, and KPMG.  
Only 05 seats left (First-come basis):

Registration Link (Nominal Training fees applicable for Training):
Apply Here

SCHOLARSHIP CODE:- SEU@2026 
(Use this code for a 50% student scholarship discount)

Key Benefits:
Candidates who fulfill the program requirements will receive verifiable credentials:

•  IBM Program Completion Certificate
•  IBM Capstone Project Certification
•  Global Skill Development Certification
•  Letter of Recommendation (Based on project performance)
(These credentials are designed to support resumes, corporate applications, and postgraduate academic pursuits).

 Please note that if you do not complete your registration before the deadline, your scholarship will be cancelled. No requests or exceptions will be accepted after the deadline.  
 
Official Training Department WhatsApp Number for Registration Process:  8197873483 

Students who have already completed the registration may disregard this communication.
By Order,
Program Coordination Team
Corizo in collaboration with IBM""","Your card application has been rejected due to average account balance is below eligibility To object you may call"
,"Transaction Declined: Your card is not activated. Please activate your card from card controls in D360 app.",
"Card ending with **6184 is temporary frozen as per your request.",
"Card ending with **6184 is unfrozen as per your request.",
"Your card has been reissued. Check the D360 app for the new card details.",
"Your bank card ending with 2188 has been added to digital wallet. Please complete the card activation process.",
"On: 2026-08-23 15:25",
"324110 is your verification code to add to digital wallet. This will expire in half an hour.",
"FURTHER REDUCTIONS ON SALE 30 %-70 % off.In store & online:","Buy one, get one on us! Use code SND96: bit.ly/SND96","URGENT: Your bank account has been locked. Verify now at bit.ly/secure-verify",
    "Hey, are we still on for lunch tomorrow?",
    "Dear customer, your PayPal account requires immediate verification. Click here.",
    "Reminder: your dentist appointment is at 3pm tomorrow.",
    "Congratulations! You've won a lottery worth $50,000. Send your bank details to claim.",


]

for msg in test_messages:
    result = classifier(msg)
    print(f"{msg}\n → {result}\n")
