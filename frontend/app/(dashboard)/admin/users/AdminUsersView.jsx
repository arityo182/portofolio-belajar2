"use client";

import { useEffect, useMemo, useState } from "react";
import {
  AlertCircle,
  CheckCircle2,
  Pencil,
  Plus,
  UserX,
  Users,
} from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import DataTable from "@/components/DataTable";
import Modal from "@/components/Modal";
import ConfirmModal from "@/components/ConfirmModal";
import Input from "@/components/ui/Input";
import Select from "@/components/ui/Select";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/hooks/useAuth";
import {
  useAdminUsers,
  useCreateUser,
  useDeactivateUser,
  useUpdateUser,
} from "@/hooks/useAdmin";
import {
  getAccountStatusLabel,
  getRoleLabel,
  ROLE_LABELS,
} from "@/lib/constants";
import { formatDateShort, getApiErrorMessage } from "@/lib/utils";

const ROLES = Object.keys(ROLE_LABELS);
const STATUSES = ["ACTIVE", "PENDING_VERIFICATION", "SUSPENDED"];

const STATUS_VARIANT = {
  ACTIVE: "success",
  PENDING_VERIFICATION: "warning",
  SUSPENDED: "danger",
};

function shortId(value) {
  return value ? String(value).slice(0, 8).toUpperCase() : "—";
}

function UserFormModal({ open, user, onClose, onSaved }) {
  const createUser = useCreateUser();
  const updateUser = useUpdateUser();
  const editing = Boolean(user);
  const [values, setValues] = useState({
    email: "",
    phone: "",
    password: "",
    role: "BORROWER",
    status: "ACTIVE",
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState("");

  useEffect(() => {
    if (!open) {
      return;
    }
    setValues({
      email: user?.email || "",
      phone: user?.phone || "",
      password: "",
      role: user?.roles?.[0] || "BORROWER",
      status: user?.status || "ACTIVE",
    });
    setErrors({});
    setFormError("");
  }, [open, user]);

  const pending = createUser.isPending || updateUser.isPending;

  function update(field, value) {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  function validate() {
    const next = {};
    if (!editing && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
      next.email = "Masukkan alamat email yang valid";
    }
    if (values.phone && values.phone.trim().length > 20) {
      next.phone = "Nomor telepon maksimal 20 karakter";
    }
    if (!editing && values.password.length < 12) {
      next.password = "Kata sandi minimal 12 karakter";
    }
    if (editing && values.password && values.password.length < 12) {
      next.password = "Kata sandi minimal 12 karakter";
    }
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function submit(event) {
    event.preventDefault();
    setFormError("");
    if (!validate()) {
      return;
    }
    try {
      if (editing) {
        await updateUser.mutateAsync({
          userId: user.userId,
          payload: {
            phone: values.phone.trim() || null,
            role: values.role,
            status: values.status,
            password: values.password || null,
          },
        });
      } else {
        await createUser.mutateAsync({
          email: values.email.trim(),
          phone: values.phone.trim() || null,
          password: values.password,
          role: values.role,
          status: values.status,
        });
      }
      onSaved(editing ? "Pengguna berhasil diperbarui." : "Pengguna baru berhasil dibuat.");
      onClose();
    } catch (error) {
      setFormError(getApiErrorMessage(error, "Gagal menyimpan pengguna."));
    }
  }

  return (
    <Modal
      open={open}
      onClose={pending ? () => {} : onClose}
      title={editing ? "Edit pengguna" : "Tambah pengguna"}
      description={
        editing
          ? "Perbarui peran, status, atau kata sandi pengguna."
          : "Buat akun baru dan tentukan peran awalnya."
      }
      size="md"
    >
      <form onSubmit={submit} className="space-y-4" noValidate>
        {formError ? (
          <div
            role="alert"
            className="flex items-start gap-2 rounded-xl border border-danger-200 bg-danger-50 px-3.5 py-3 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
          >
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
            {formError}
          </div>
        ) : null}

        <Input
          label="Email"
          type="email"
          value={values.email}
          onChange={(event) => update("email", event.target.value)}
          error={errors.email}
          disabled={editing}
          required={!editing}
          autoComplete="off"
        />
        <Input
          label="Nomor telepon"
          type="tel"
          value={values.phone}
          onChange={(event) => update("phone", event.target.value)}
          error={errors.phone}
          placeholder="+6281234567890"
          autoComplete="off"
        />
        <Input
          label={editing ? "Kata sandi baru (opsional)" : "Kata sandi"}
          type="password"
          value={values.password}
          onChange={(event) => update("password", event.target.value)}
          error={errors.password}
          helper={
            editing
              ? "Kosongkan jika tidak ingin mengubah kata sandi."
              : "Minimal 12 karakter. Gunakan kombinasi huruf, angka, dan simbol."
          }
          required={!editing}
          autoComplete="new-password"
        />
        <div className="grid gap-3 sm:grid-cols-2">
          <Select
            label="Peran"
            value={values.role}
            onChange={(event) => update("role", event.target.value)}
            required
          >
            {ROLES.map((role) => (
              <option key={role} value={role}>
                {getRoleLabel(role)}
              </option>
            ))}
          </Select>
          <Select
            label="Status"
            value={values.status}
            onChange={(event) => update("status", event.target.value)}
            required
          >
            {STATUSES.map((status) => (
              <option key={status} value={status}>
                {getAccountStatusLabel(status)}
              </option>
            ))}
          </Select>
        </div>

        <div className="flex justify-end gap-2.5 border-t border-line pt-4">
          <Button type="button" variant="secondary" onClick={onClose} disabled={pending}>
            Batal
          </Button>
          <Button type="submit" isLoading={pending} loadingText="Menyimpan...">
            {editing ? "Simpan perubahan" : "Buat pengguna"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default function AdminUsersView() {
  const { user: currentUser } = useAuth();
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [formOpen, setFormOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [deactivating, setDeactivating] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const usersQuery = useAdminUsers({ role, status, size: 100 });
  const deactivateUser = useDeactivateUser();
  const users = useMemo(() => usersQuery.data?.content || [], [usersQuery.data]);

  function openCreate() {
    setEditingUser(null);
    setFormOpen(true);
  }

  function openEdit(user) {
    setEditingUser(user);
    setFormOpen(true);
  }

  async function confirmDeactivate() {
    setError("");
    try {
      await deactivateUser.mutateAsync(deactivating.userId);
      setDeactivating(null);
      setMessage("Pengguna berhasil dinonaktifkan.");
    } catch (err) {
      setDeactivating(null);
      setError(getApiErrorMessage(err, "Gagal menonaktifkan pengguna."));
    }
  }

  const columns = [
    {
      key: "email",
      header: "Pengguna",
      sortable: true,
      render: (row) => (
        <div>
          <p className="text-sm font-semibold text-content">{row.email}</p>
          <p className="numeric mt-0.5 text-2xs text-content-subtle">
            #{shortId(row.userId)} · {row.phone || "tanpa telepon"}
          </p>
        </div>
      ),
      exportValue: (row) => row.email,
    },
    {
      key: "roles",
      header: "Peran",
      render: (row) => (
        <div className="flex flex-wrap gap-1">
          {(row.roles || []).map((item) => (
            <Badge key={item} variant="primary" size="sm">
              {getRoleLabel(item)}
            </Badge>
          ))}
        </div>
      ),
      exportValue: (row) => (row.roles || []).join(", "),
    },
    {
      key: "status",
      header: "Status",
      sortable: true,
      render: (row) => (
        <Badge variant={STATUS_VARIANT[row.status] || "neutral"} dot>
          {getAccountStatusLabel(row.status)}
        </Badge>
      ),
      exportValue: (row) => row.status,
    },
    {
      key: "createdAt",
      header: "Bergabung",
      sortable: true,
      render: (row) => (
        <span className="whitespace-nowrap text-sm text-content-muted">
          {formatDateShort(row.createdAt)}
        </span>
      ),
      exportValue: (row) => row.createdAt,
    },
    {
      key: "actions",
      header: "Aksi",
      align: "right",
      render: (row) => {
        const isSelf = row.userId === currentUser?.userId;
        return (
          <div className="flex justify-end gap-1">
            <button
              type="button"
              onClick={() => openEdit(row)}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-surface-subtle hover:text-primary-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:hover:text-primary-300"
              aria-label={`Edit pengguna ${row.email}`}
            >
              <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
            <button
              type="button"
              onClick={() => setDeactivating(row)}
              disabled={row.status === "SUSPENDED" || isSelf}
              className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-danger-50 hover:text-danger-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-danger-600 disabled:pointer-events-none disabled:opacity-40 dark:hover:bg-danger-700/15"
              aria-label={`Nonaktifkan pengguna ${row.email}`}
              title={isSelf ? "Tidak dapat menonaktifkan akun sendiri" : "Nonaktifkan pengguna"}
            >
              <UserX className="h-3.5 w-3.5" aria-hidden="true" />
            </button>
          </div>
        );
      },
      exportable: false,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Administrasi"
        title="Manajemen pengguna"
        description="Kelola akun, peran, status verifikasi, dan akses pengguna platform."
        actions={
          <Button onClick={openCreate} leftIcon={<Plus className="h-4 w-4" />}>
            Tambah pengguna
          </Button>
        }
      />

      {message ? (
        <div
          role="status"
          className="flex items-center gap-2 rounded-xl border border-success-200 bg-success-50 px-4 py-3 text-sm text-success-700 dark:border-success-700/40 dark:bg-success-700/15 dark:text-success-200"
        >
          <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
          {message}
        </div>
      ) : null}
      {error ? (
        <div
          role="alert"
          className="flex items-center gap-2 rounded-xl border border-danger-200 bg-danger-50 px-4 py-3 text-sm text-danger-700 dark:border-danger-700/40 dark:bg-danger-700/15 dark:text-danger-200"
        >
          <AlertCircle className="h-4 w-4" aria-hidden="true" />
          {error}
        </div>
      ) : null}

      {usersQuery.isError ? (
        <Card>
          <CardContent className="py-10 text-center">
            <Users className="mx-auto h-6 w-6 text-content-subtle" aria-hidden="true" />
            <p className="mt-2 text-sm font-semibold text-content">Gagal memuat pengguna</p>
            <Button variant="secondary" className="mt-4" onClick={() => usersQuery.refetch()}>
              Coba lagi
            </Button>
          </CardContent>
        </Card>
      ) : (
        <DataTable
          columns={columns}
          data={users}
          rowKey={(row) => row.userId}
          loading={usersQuery.isLoading}
          caption="Daftar pengguna platform"
          searchable
          searchPlaceholder="Cari email atau nomor telepon..."
          searchKeys={["email", "phone", "userId"]}
          pageSize={10}
          exportFileName="pengguna-koperasi.csv"
          emptyTitle="Tidak ada pengguna"
          emptyDescription="Ubah filter atau tambahkan pengguna baru."
          filters={
            <>
              <select
                value={role}
                onChange={(event) => setRole(event.target.value)}
                aria-label="Filter peran"
                className="h-10 rounded-xl border border-line bg-surface-raised px-3 text-sm text-content shadow-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25"
              >
                <option value="">Semua peran</option>
                {ROLES.map((item) => (
                  <option key={item} value={item}>
                    {getRoleLabel(item)}
                  </option>
                ))}
              </select>
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value)}
                aria-label="Filter status"
                className="h-10 rounded-xl border border-line bg-surface-raised px-3 text-sm text-content shadow-sm outline-none focus:border-primary-500 focus:ring-2 focus:ring-primary-600/25"
              >
                <option value="">Semua status</option>
                {STATUSES.map((item) => (
                  <option key={item} value={item}>
                    {getAccountStatusLabel(item)}
                  </option>
                ))}
              </select>
            </>
          }
        />
      )}

      <UserFormModal
        open={formOpen}
        user={editingUser}
        onClose={() => setFormOpen(false)}
        onSaved={(nextMessage) => setMessage(nextMessage)}
      />

      <ConfirmModal
        open={Boolean(deactivating)}
        title="Nonaktifkan pengguna?"
        description={
          deactivating
            ? `${deactivating.email} tidak akan dapat masuk hingga diaktifkan kembali.`
            : undefined
        }
        confirmLabel="Ya, nonaktifkan"
        variant="danger"
        loading={deactivateUser.isPending}
        onConfirm={confirmDeactivate}
        onClose={() => setDeactivating(null)}
      />
    </div>
  );
}
