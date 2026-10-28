"use client";

import { useEffect, useMemo, useState } from "react";
import { AlertCircle, CheckCircle2, Pencil, Plus, Power, Wallet } from "lucide-react";
import PageHeader from "@/components/ui/PageHeader";
import { Card, CardContent } from "@/components/ui/Card";
import DataTable from "@/components/DataTable";
import Modal from "@/components/Modal";
import ConfirmModal from "@/components/ConfirmModal";
import Input from "@/components/ui/Input";
import Badge from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import {
  useAdminProducts,
  useCreateProduct,
  useDeactivateProduct,
  useUpdateProduct,
} from "@/hooks/useAdmin";
import { formatCurrency, getApiErrorMessage } from "@/lib/utils";

function ProductFormModal({ open, product, onClose, onSaved }) {
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct();
  const editing = Boolean(product);
  const [values, setValues] = useState({
    name: "",
    minAmount: "1000000",
    maxAmount: "50000000",
    interestRateAnnual: "18",
    minTenorMonths: "3",
    maxTenorMonths: "24",
    active: true,
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState("");

  useEffect(() => {
    if (!open) {
      return;
    }
    setValues({
      name: product?.name || "",
      minAmount: String(product?.minAmount ?? "1000000"),
      maxAmount: String(product?.maxAmount ?? "50000000"),
      interestRateAnnual: String(product?.interestRateAnnual ?? "18"),
      minTenorMonths: String(product?.minTenorMonths ?? "3"),
      maxTenorMonths: String(product?.maxTenorMonths ?? "24"),
      active: product?.isActive ?? true,
    });
    setErrors({});
    setFormError("");
  }, [open, product]);

  const pending = createProduct.isPending || updateProduct.isPending;

  function update(field, value) {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  function validate() {
    const next = {};
    const minAmount = Number(values.minAmount);
    const maxAmount = Number(values.maxAmount);
    const rate = Number(values.interestRateAnnual);
    const minTenor = Number(values.minTenorMonths);
    const maxTenor = Number(values.maxTenorMonths);

    if (!values.name.trim()) {
      next.name = "Nama produk wajib diisi";
    }
    if (!Number.isFinite(minAmount) || minAmount < 1) {
      next.minAmount = "Nominal minimum tidak valid";
    }
    if (!Number.isFinite(maxAmount) || maxAmount < minAmount) {
      next.maxAmount = "Nominal maksimum harus ≥ nominal minimum";
    }
    if (!Number.isFinite(rate) || rate < 0 || rate > 100) {
      next.interestRateAnnual = "Suku bunga harus antara 0 dan 100";
    }
    if (!Number.isInteger(minTenor) || minTenor < 1 || minTenor > 60) {
      next.minTenorMonths = "Tenor minimum 1–60 bulan";
    }
    if (!Number.isInteger(maxTenor) || maxTenor < minTenor || maxTenor > 60) {
      next.maxTenorMonths = "Tenor maksimum harus ≥ minimum dan ≤ 60";
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
    const payload = {
      name: values.name.trim(),
      minAmount: Number(values.minAmount),
      maxAmount: Number(values.maxAmount),
      interestRateAnnual: Number(values.interestRateAnnual),
      minTenorMonths: Number(values.minTenorMonths),
      maxTenorMonths: Number(values.maxTenorMonths),
      active: Boolean(values.active),
    };
    try {
      if (editing) {
        await updateProduct.mutateAsync({ productId: product.id, payload });
      } else {
        await createProduct.mutateAsync(payload);
      }
      onSaved(editing ? "Produk berhasil diperbarui." : "Produk baru berhasil dibuat.");
      onClose();
    } catch (error) {
      setFormError(getApiErrorMessage(error, "Gagal menyimpan produk pinjaman."));
    }
  }

  return (
    <Modal
      open={open}
      onClose={pending ? () => {} : onClose}
      title={editing ? "Edit produk pinjaman" : "Tambah produk pinjaman"}
      description="Atur plafon, suku bunga, dan rentang tenor yang tersedia untuk peminjam."
      size="lg"
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
          label="Nama produk"
          value={values.name}
          onChange={(event) => update("name", event.target.value)}
          error={errors.name}
          required
        />
        <div className="grid gap-3 sm:grid-cols-2">
          <Input
            label="Nominal minimum (Rp)"
            type="number"
            min="1"
            step="100000"
            value={values.minAmount}
            onChange={(event) => update("minAmount", event.target.value)}
            error={errors.minAmount}
            required
            className="numeric"
          />
          <Input
            label="Nominal maksimum (Rp)"
            type="number"
            min="1"
            step="100000"
            value={values.maxAmount}
            onChange={(event) => update("maxAmount", event.target.value)}
            error={errors.maxAmount}
            required
            className="numeric"
          />
          <Input
            label="Suku bunga tahunan (%)"
            type="number"
            min="0"
            max="100"
            step="0.01"
            value={values.interestRateAnnual}
            onChange={(event) => update("interestRateAnnual", event.target.value)}
            error={errors.interestRateAnnual}
            required
            className="numeric"
          />
          <div className="grid grid-cols-2 gap-3">
            <Input
              label="Tenor min."
              type="number"
              min="1"
              max="60"
              value={values.minTenorMonths}
              onChange={(event) => update("minTenorMonths", event.target.value)}
              error={errors.minTenorMonths}
              required
              className="numeric"
            />
            <Input
              label="Tenor maks."
              type="number"
              min="1"
              max="60"
              value={values.maxTenorMonths}
              onChange={(event) => update("maxTenorMonths", event.target.value)}
              error={errors.maxTenorMonths}
              required
              className="numeric"
            />
          </div>
        </div>

        <label className="flex items-start gap-2.5 rounded-xl border border-line bg-surface-raised p-3.5">
          <input
            type="checkbox"
            checked={values.active}
            onChange={(event) => update("active", event.target.checked)}
            className="mt-0.5 h-4 w-4 rounded border-neutral-300 text-primary-600 focus:ring-2 focus:ring-primary-600 dark:border-neutral-600 dark:bg-neutral-800"
          />
          <span className="text-xs leading-5 text-content-muted">
            Produk aktif dan dapat dipilih peminjam saat mengajukan pinjaman.
          </span>
        </label>

        <div className="flex justify-end gap-2.5 border-t border-line pt-4">
          <Button type="button" variant="secondary" onClick={onClose} disabled={pending}>
            Batal
          </Button>
          <Button type="submit" isLoading={pending} loadingText="Menyimpan...">
            {editing ? "Simpan perubahan" : "Buat produk"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}

export default function AdminLoanProductsView() {
  const productsQuery = useAdminProducts();
  const deactivateProduct = useDeactivateProduct();
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deactivating, setDeactivating] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const products = useMemo(() => productsQuery.data || [], [productsQuery.data]);

  function openCreate() {
    setEditing(null);
    setFormOpen(true);
  }

  function openEdit(product) {
    setEditing(product);
    setFormOpen(true);
  }

  async function confirmDeactivate() {
    setError("");
    try {
      await deactivateProduct.mutateAsync(deactivating.id);
      setDeactivating(null);
      setMessage("Produk berhasil dinonaktifkan.");
    } catch (err) {
      setDeactivating(null);
      setError(getApiErrorMessage(err, "Gagal menonaktifkan produk."));
    }
  }

  const columns = [
    {
      key: "name",
      header: "Produk",
      sortable: true,
      render: (row) => (
        <div>
          <p className="text-sm font-semibold text-content">{row.name}</p>
          <p className="text-2xs text-content-subtle">ID #{row.id}</p>
        </div>
      ),
      exportValue: (row) => row.name,
    },
    {
      key: "minAmount",
      header: "Plafon",
      sortable: true,
      render: (row) => (
        <span className="numeric text-xs text-content">
          {formatCurrency(row.minAmount)} – {formatCurrency(row.maxAmount)}
        </span>
      ),
      exportValue: (row) => `${row.minAmount}-${row.maxAmount}`,
    },
    {
      key: "interestRateAnnual",
      header: "Bunga / tahun",
      sortable: true,
      align: "right",
      render: (row) => (
        <span className="numeric font-semibold text-content">
          {Number(row.interestRateAnnual).toFixed(2).replace(".", ",")}%
        </span>
      ),
      exportValue: (row) => row.interestRateAnnual,
    },
    {
      key: "tenor",
      header: "Tenor",
      render: (row) => (
        <span className="numeric text-sm text-content-muted">
          {row.minTenorMonths}–{row.maxTenorMonths} bulan
        </span>
      ),
      exportValue: (row) => `${row.minTenorMonths}-${row.maxTenorMonths}`,
    },
    {
      key: "isActive",
      header: "Status",
      sortable: true,
      render: (row) => (
        <Badge variant={row.isActive ? "success" : "neutral"} dot>
          {row.isActive ? "Aktif" : "Nonaktif"}
        </Badge>
      ),
      exportValue: (row) => (row.isActive ? "Aktif" : "Nonaktif"),
    },
    {
      key: "actions",
      header: "Aksi",
      align: "right",
      render: (row) => (
        <div className="flex justify-end gap-1">
          <button
            type="button"
            onClick={() => openEdit(row)}
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-surface-subtle hover:text-primary-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-600 dark:hover:text-primary-300"
            aria-label={`Edit produk ${row.name}`}
          >
            <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
          </button>
          <button
            type="button"
            onClick={() => setDeactivating(row)}
            disabled={!row.isActive}
            className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-content-muted transition-colors hover:bg-danger-50 hover:text-danger-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-danger-600 disabled:pointer-events-none disabled:opacity-40 dark:hover:bg-danger-700/15"
            aria-label={`Nonaktifkan produk ${row.name}`}
          >
            <Power className="h-3.5 w-3.5" aria-hidden="true" />
          </button>
        </div>
      ),
      exportable: false,
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Administrasi"
        title="Produk pinjaman"
        description="Kelola produk pembiayaan, plafon, bunga, dan tenor yang ditawarkan."
        actions={
          <Button onClick={openCreate} leftIcon={<Plus className="h-4 w-4" />}>
            Tambah produk
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

      {productsQuery.isError ? (
        <Card>
          <CardContent className="py-10 text-center">
            <Wallet className="mx-auto h-6 w-6 text-content-subtle" aria-hidden="true" />
            <p className="mt-2 text-sm font-semibold text-content">Gagal memuat produk</p>
            <Button variant="secondary" className="mt-4" onClick={() => productsQuery.refetch()}>
              Coba lagi
            </Button>
          </CardContent>
        </Card>
      ) : (
        <DataTable
          columns={columns}
          data={products}
          rowKey={(row) => row.id}
          loading={productsQuery.isLoading}
          caption="Daftar produk pinjaman"
          searchable
          searchPlaceholder="Cari nama produk..."
          searchKeys={["name"]}
          pageSize={10}
          exportFileName="produk-pinjaman.csv"
          emptyTitle="Belum ada produk"
          emptyDescription="Tambahkan produk pinjaman pertama."
        />
      )}

      <ProductFormModal
        open={formOpen}
        product={editing}
        onClose={() => setFormOpen(false)}
        onSaved={(nextMessage) => setMessage(nextMessage)}
      />

      <ConfirmModal
        open={Boolean(deactivating)}
        title="Nonaktifkan produk?"
        description={
          deactivating
            ? `${deactivating.name} tidak akan muncul pada pilihan pengajuan baru.`
            : undefined
        }
        confirmLabel="Ya, nonaktifkan"
        variant="danger"
        loading={deactivateProduct.isPending}
        onConfirm={confirmDeactivate}
        onClose={() => setDeactivating(null)}
      />
    </div>
  );
}
