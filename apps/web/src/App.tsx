import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { AdminLayout } from './layouts/AdminLayout';
import { AdminManagementPage } from './pages/AdminManagementPage';
import { AssetManagementPage } from './pages/AssetManagementPage';
import { AuditLogsPage } from './pages/AuditLogsPage';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { ReconciliationPage } from './pages/ReconciliationPage';
import { RiskSettingsPage } from './pages/RiskSettingsPage';
import { ScannerStatusPage } from './pages/ScannerStatusPage';
import { SystemStatusPage } from './pages/SystemStatusPage';
import { UserDetailPage } from './pages/UserDetailPage';
import { UsersPage } from './pages/UsersPage';
import { WithdrawalReviewPage } from './pages/WithdrawalReviewPage';

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route path="/" element={<AdminLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="users/:userId" element={<UserDetailPage />} />
            <Route path="withdrawals/review" element={<WithdrawalReviewPage />} />
            <Route path="scanner/status" element={<ScannerStatusPage />} />
            <Route path="risk/settings" element={<RiskSettingsPage />} />
            <Route path="assets" element={<AssetManagementPage />} />
            <Route path="admin-management" element={<AdminManagementPage />} />
            <Route path="system/status" element={<SystemStatusPage />} />
            <Route path="audit-logs" element={<AuditLogsPage />} />
            <Route path="reconciliation" element={<ReconciliationPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
