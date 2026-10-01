# app/application/use_cases.py
from app.domain.models import Patient
from app.domain.ports import PatientRepository

class PatientService:
    def __init__(self, repository: PatientRepository):
        self.repository = repository

    def create_patient(self, patient: Patient) -> Patient:
        # rest of the bussiness logic (ex. verify if the patient alr exist)
        return self.repository.create(patient)

    def get_patient(self, patient_id: int) -> Patient:
        return self.repository.get_by_id(patient_id)