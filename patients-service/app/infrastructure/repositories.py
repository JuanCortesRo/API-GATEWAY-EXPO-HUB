# app/infrastructure/repositories.py
from sqlalchemy.orm import Session
from app.domain.ports import PatientRepository
from app.domain.models import Patient
from app.infrastructure.orm_models import PatientEntity

class SQLAlchemyPatientRepository(PatientRepository):
    def __init__(self, db: Session):
        self.db = db

    def create(self, patient: Patient) -> Patient:
        db_patient = PatientEntity(
            name=patient.name,
            document_id=patient.document_id,
            email=patient.email
        )
        self.db.add(db_patient)
        self.db.commit()
        self.db.refresh(db_patient)
        
        return Patient(
            id=db_patient.id,
            name=db_patient.name,
            document_id=db_patient.document_id,
            email=db_patient.email
        )

    def get_by_id(self, patient_id: int) -> Patient | None:
        db_patient = self.db.query(PatientEntity).filter(PatientEntity.id == patient_id).first()
        if db_patient:
            return Patient(
                id=db_patient.id,
                name=db_patient.name,
                document_id=db_patient.document_id,
                email=db_patient.email
            )
        return None