# app/infrastructure/orm_models.py
from sqlalchemy import Column, Integer, String
from .database import Base

class PatientEntity(Base):
    __tablename__ = "patients"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, index=True)
    document_id = Column(String, unique=True, index=True)
    email = Column(String, unique=True, index=True)