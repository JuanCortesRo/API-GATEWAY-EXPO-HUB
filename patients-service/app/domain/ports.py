# app/domain/ports.py
from abc import ABC, abstractmethod
from typing import Optional
from .models import Patient

class PatientRepository(ABC):
    @abstractmethod
    def create(self, patient: Patient) -> Patient:
        pass

    @abstractmethod
    def get_by_id(self, patient_id: int) -> Optional[Patient]:
        pass