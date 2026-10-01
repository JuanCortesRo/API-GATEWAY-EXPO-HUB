# app/domain/models.py
from pydantic import BaseModel
from typing import Optional

class Patient(BaseModel):
    id: Optional[int] = None
    name: str
    document_id: str
    email: str