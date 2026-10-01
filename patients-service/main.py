# main.py
from fastapi import FastAPI, Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.domain.models import Patient
from app.application.use_cases import PatientService
from app.infrastructure.database import get_db, engine, Base
from app.infrastructure.repositories import SQLAlchemyPatientRepository

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Microservicio de Pacientes", version="1.0.0")

# --- dependencies injection ---
def get_patient_service(db: Session = Depends(get_db)) -> PatientService:
    repository = SQLAlchemyPatientRepository(db)
    return PatientService(repository)

# --- Controllers (Endpoints HTTP) ---

@app.post("/patients/", response_model=Patient, status_code=201)
def create_patient(
    patient: Patient,
    x_patient_id: str = Header(..., alias="X-Patient-Id"),
    service: PatientService = Depends(get_patient_service)
):
    try:
        return service.create_patient(patient, x_patient_id)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/patients/me", response_model=Patient)
def get_me(
    x_patient_id: str = Header(..., alias="X-Patient-Id"),
    service: PatientService = Depends(get_patient_service)
):
    patient = service.get_patient(x_patient_id)
    if not patient:
        raise HTTPException(status_code=404, detail="Paciente no encontrado")
    return patient
