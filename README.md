# Pharmacy Prescription Management Database

The Pharmacy Prescription Management Database is a relational database project for a web application that manages doctors, patients, prescription drugs, pharmacies, prescriptions, prescription fills, and pharmacy-specific drug pricing. The project models the complete prescription workflow from a doctor prescribing medication to a patient receiving a prescription fill from a pharmacy.

## Project Overview

The system supports healthcare and pharmacy operations by connecting:

- Doctors with their assigned patients
- Patients with their prescribing doctors
- Prescriptions with patients, doctors, and drugs
- Pharmacies with the drugs they stock
- Prescription fills with pharmacies, dates, and medication costs

The database also supports validation scenarios such as registering patients, creating prescriptions, filling prescriptions, editing patient records, and displaying meaningful errors when invalid doctors, drugs, pharmacies, or prescription IDs are entered.

## Database Schema

The database is organized around the following tables:

- **Doctor** – Stores provider identification, name, specialty, practice history, and a unique SSN.
- **Patient** – Stores patient identity, contact information, birthdate, SSN, and assigned doctor.
- **Drug** – Stores the catalog of prescription drug names.
- **Pharmacy** – Stores pharmacy names, addresses, and phone numbers.
- **Prescription** – Connects a doctor, patient, and drug while recording quantity, prescription date, and permitted refills.
- **Prescription_Fill** – Records when and where a prescription was filled and the final fill price.
- **Drug_Cost** – Stores medication pricing by drug, pharmacy, and unit amount.

Foreign-key relationships enforce data integrity between doctors, patients, prescriptions, drugs, pharmacies, and prescription fills. Primary keys, unique indexes, and supporting indexes are used to identify records and improve relationship-based queries.

## Sample Data

The project includes sample data for:

- Walgreens and CVS pharmacy locations
- Common prescription drugs such as lisinopril, loratadine, acetaminophen, lovastatin, Xanax, hydrocodone, and oxycodone
- Multiple package sizes and prices for each pharmacy

The sample pricing data demonstrates how the same medication may have different prices depending on the pharmacy and quantity purchased.

## Demonstrated Workflows

The project documents and validates the following application workflows:

1. Register a new patient with a valid doctor.
2. Reject patient registration when the doctor does not exist.
3. Create a prescription for a valid patient, doctor, and drug.
4. Reject a prescription containing an invalid drug.
5. Reject a prescription fill containing an invalid pharmacy.
6. Reject a prescription fill containing an invalid prescription ID.
7. Successfully fill a valid prescription.
8. Retrieve and update a patient profile.
9. Reject a patient update when the new doctor does not exist.

## Technical Skills Demonstrated

- Relational database design
- Entity-relationship modeling
- SQL schema creation and data population
- Primary-key and foreign-key design
- Unique and supporting indexes
- Many-to-one relationships between domain entities
- Data validation and referential integrity
- Prescription and pharmacy workflow modeling
- Testing successful and unsuccessful application scenarios

## Team Project

This project was completed collaboratively by the Replicant Collective team. Team members contributed to the database model, SQL schema, application workflows, validation cases, and project documentation.

## Project Information

- Course: CST 363 – Database Systems
- Project: Lab 19 – Pharmacy Web Application Database
- Database schema: `prescription`
