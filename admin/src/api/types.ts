export interface UserDto {
  id: number;
  name: string;
  phone: string;
  language: string;
}

export interface UserSessionDto {
  id: number;
  userId: number;
  userName: string;
  currentState: string;
  lastActive: string;
  sessionData?: string;
}

export interface MedicineDto {
  name: string;
  dosage: string;
  frequency: string;
  days: number;
}

export interface ConsultationDto {
  id: number;
  userId: number;
  doctorId?: number;
  type: string;
  status: string;
  startTime: string;
  endTime?: string;
  notes?: string;
}

export interface PrescriptionDto {
  id: number;
  consultationId: number;
  medicines: MedicineDto[];
  createdAt: string;
}

export interface PurchaseDto {
  id: number;
  prescriptionId: number;
  userId: number;
  status: string;
  totalAmount: number;
  purchasedAt?: string;
  createdAt: string;
}

export interface DoctorDto {
  id: number;
  name: string;
  specialty: string;
}

export interface ActiveUserEntry {
  user: UserDto;
  session: UserSessionDto;
  latestConsultation?: ConsultationDto;
  latestPrescription?: PrescriptionDto;
  latestPurchase?: PurchaseDto;
}

export interface ConsultationEntry {
  consultation: ConsultationDto;
  user: UserDto;
  doctor?: DoctorDto;
  prescription?: PrescriptionDto;
}

export interface PurchaseEntry {
  purchase: PurchaseDto;
  user: UserDto;
  prescription?: PrescriptionDto;
}

export interface LlmConfigDto {
  provider: string;
  model: string;
  apiKeyMasked: string;
  mockMode: boolean;
  mockScript: string; // MEDICATION | ONLINE_CONSULTATION | OFFLINE_APPOINTMENT
}
