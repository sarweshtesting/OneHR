export const ROLE_LABELS = {
  SUPER_ADMIN: 'Super Admin',
  MANAGER: 'Manager',
  HR_ADMIN: 'HR Admin',
  EMPLOYEE: 'Employee User',
  PLATFORM_ADMIN: 'Platform Admin',
};

export function roleLabel(role) {
  return ROLE_LABELS[role] || role;
}

const MANAGER_UP = ['MANAGER', 'HR_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN'];
const ORG_WIDE = ['HR_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN'];
const FINANCE_ACCESS = ['MANAGER', 'HR_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN'];
const CAN_MANAGE_PEOPLE = ['HR_ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN'];

export function isManagerUp(role) {
  return MANAGER_UP.includes(role);
}

export function isOrgWide(role) {
  return ORG_WIDE.includes(role);
}

export function canAccessFinance(role) {
  return FINANCE_ACCESS.includes(role);
}

export function canManagePeople(role) {
  return CAN_MANAGE_PEOPLE.includes(role);
}
