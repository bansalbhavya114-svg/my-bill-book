-- ==============================================================================
-- Supabase PostgreSQL Schema for BillBook (Android App + Cloud Sync)
-- Target Project: bansalbhavya114@gmail.com's project
-- Project ID: srpuwmqmxvwjdyyjgueh
-- Project URL: https://srpuwmqmxvwjdyyjgueh.supabase.co
--
-- Instructions:
-- 1. Open Supabase SQL Editor: https://supabase.com/dashboard/project/srpuwmqmxvwjdyyjgueh/sql
-- 2. Click "New Query", paste this entire script, and click "RUN".
-- ==============================================================================

-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================================================
-- Table 1: Businesses / Company Profiles (`businesses` & `business_profiles`)
-- Stores business registration, GST details, bank/UPI account, and invoice settings
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.businesses (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    business_name TEXT NOT NULL,
    trade_name TEXT DEFAULT '',
    email TEXT NOT NULL,
    phone TEXT DEFAULT '',
    gstin TEXT DEFAULT '',
    business_type TEXT DEFAULT 'Retail',
    turnover TEXT DEFAULT 'Less than ₹20 Lakhs',
    address TEXT DEFAULT '',
    state TEXT DEFAULT 'Delhi',
    state_code TEXT DEFAULT '07',
    upi_id TEXT DEFAULT '',
    bank_name TEXT DEFAULT '',
    account_number TEXT DEFAULT '',
    ifsc_code TEXT DEFAULT '',
    invoice_prefix TEXT DEFAULT 'INV-26-',
    next_invoice_number INT DEFAULT 101,
    currency_symbol TEXT DEFAULT '₹',
    terms_conditions TEXT DEFAULT '1. Goods once sold will not be taken back.
2. Interest @18% p.a. will be charged if bill is not paid on due date.
3. Subject to local jurisdiction only.',
    signature_title TEXT DEFAULT 'Authorized Signatory',
    role TEXT DEFAULT 'OWNER',
    financial_year TEXT DEFAULT 'FY 2026-27',
    is_current BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- Table 2: Items / Products & Inventory (`items`)
-- Stores catalog, HSN, stock quantity, MRP, sale/purchase price, and tax rate
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.items (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT REFERENCES public.businesses(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    sku TEXT DEFAULT '',
    hsn_code TEXT DEFAULT '',
    category TEXT DEFAULT 'General',
    unit TEXT DEFAULT 'PCS',
    mrp NUMERIC(12, 2) DEFAULT 0.00,
    sale_price NUMERIC(12, 2) DEFAULT 0.00,
    purchase_price NUMERIC(12, 2) DEFAULT 0.00,
    tax_rate NUMERIC(5, 2) DEFAULT 18.00,
    stock_qty NUMERIC(12, 2) DEFAULT 0.00,
    min_stock_alert NUMERIC(12, 2) DEFAULT 5.00,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- Table 3: Parties / Customers & Suppliers (`parties`)
-- Stores customer and vendor directory, GSTIN, balances, credit limits & payment terms
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.parties (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT REFERENCES public.businesses(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT DEFAULT 'CUSTOMER', -- 'CUSTOMER' or 'SUPPLIER'
    phone TEXT DEFAULT '',
    email TEXT DEFAULT '',
    gstin TEXT DEFAULT '',
    address TEXT DEFAULT '',
    state TEXT DEFAULT 'Delhi',
    state_code TEXT DEFAULT '07',
    opening_balance NUMERIC(12, 2) DEFAULT 0.00,
    current_balance NUMERIC(12, 2) DEFAULT 0.00,
    credit_limit NUMERIC(12, 2) DEFAULT 50000.00,
    payment_terms_days INT DEFAULT 30,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- Table 4: Invoices / Orders / Checkout Data (`invoices`)
-- Stores GST invoices, bills of supply, quotations, checkout totals, taxes & payment status
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.invoices (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT REFERENCES public.businesses(id) ON DELETE CASCADE,
    party_id BIGINT REFERENCES public.parties(id) ON DELETE SET NULL,
    party_name TEXT NOT NULL DEFAULT 'Cash Customer',
    party_gstin TEXT DEFAULT '',
    party_phone TEXT DEFAULT '',
    party_address TEXT DEFAULT '',
    party_state TEXT DEFAULT 'Delhi',
    invoice_number TEXT NOT NULL,
    type TEXT DEFAULT 'GST_INVOICE', -- 'GST_INVOICE', 'BILL_OF_SUPPLY', 'ESTIMATE_QUOTE', 'PROFORMA', 'CREDIT_NOTE'
    date BIGINT NOT NULL,            -- Epoch timestamp in ms (e.g. 1774886400000)
    due_date BIGINT NOT NULL,        -- Epoch timestamp in ms
    status TEXT DEFAULT 'PAID',       -- 'PAID', 'UNPAID', 'PARTIAL', 'DRAFT', 'OVERDUE'
    subtotal NUMERIC(12, 2) DEFAULT 0.00,
    total_discount NUMERIC(12, 2) DEFAULT 0.00,
    cgst NUMERIC(12, 2) DEFAULT 0.00,
    cgst_amount NUMERIC(12, 2) DEFAULT 0.00,
    sgst NUMERIC(12, 2) DEFAULT 0.00,
    sgst_amount NUMERIC(12, 2) DEFAULT 0.00,
    igst NUMERIC(12, 2) DEFAULT 0.00,
    igst_amount NUMERIC(12, 2) DEFAULT 0.00,
    total_tax NUMERIC(12, 2) DEFAULT 0.00,
    shipping_charges NUMERIC(12, 2) DEFAULT 0.00,
    round_off NUMERIC(6, 2) DEFAULT 0.00,
    grand_total NUMERIC(12, 2) DEFAULT 0.00,
    paid_amount NUMERIC(12, 2) DEFAULT 0.00,
    payment_mode TEXT DEFAULT 'CASH', -- 'CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'CARD'
    notes TEXT DEFAULT 'Thank you for your business!',
    template_style TEXT DEFAULT 'MODERN', -- 'MODERN', 'CLASSIC', 'MINIMAL', 'GST_COMPLIANT'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- Table 5: Invoice Line Items / Cart Items (`invoice_items`)
-- Stores itemized details, quantities, rates, discounts, HSN, and GST breakdowns
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT REFERENCES public.invoices(id) ON DELETE CASCADE,
    item_id BIGINT REFERENCES public.items(id) ON DELETE SET NULL,
    item_name TEXT NOT NULL,
    hsn_code TEXT DEFAULT '',
    unit TEXT DEFAULT 'PCS',
    quantity NUMERIC(12, 2) DEFAULT 1.00,
    rate NUMERIC(12, 2) DEFAULT 0.00,
    discount_percent NUMERIC(5, 2) DEFAULT 0.00,
    tax_rate NUMERIC(5, 2) DEFAULT 18.00,
    gst_rate NUMERIC(5, 2) DEFAULT 18.00,
    amount NUMERIC(12, 2) DEFAULT 0.00,
    taxable_amount NUMERIC(12, 2) DEFAULT 0.00,
    cgst NUMERIC(12, 2) DEFAULT 0.00,
    sgst NUMERIC(12, 2) DEFAULT 0.00,
    igst NUMERIC(12, 2) DEFAULT 0.00,
    total_amount NUMERIC(12, 2) DEFAULT 0.00,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- Table 6: Payments (`payments`)
-- Stores payment records (Payment In from customers, Payment Out to vendors)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.payments (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT REFERENCES public.businesses(id) ON DELETE CASCADE,
    invoice_id BIGINT REFERENCES public.invoices(id) ON DELETE SET NULL,
    party_id BIGINT REFERENCES public.parties(id) ON DELETE SET NULL,
    party_name TEXT DEFAULT '',
    type TEXT DEFAULT 'PAYMENT_IN',   -- 'PAYMENT_IN', 'PAYMENT_OUT'
    amount NUMERIC(12, 2) NOT NULL,
    payment_mode TEXT DEFAULT 'UPI',  -- 'UPI', 'CASH', 'BANK_TRANSFER', 'CHEQUE', 'CARD'
    mode TEXT DEFAULT 'UPI',          -- backward-compatibility alias
    reference_no TEXT DEFAULT '',
    reference_number TEXT DEFAULT '', -- backward-compatibility alias
    notes TEXT DEFAULT '',
    date BIGINT NOT NULL,             -- Epoch timestamp in ms
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- Table 7: Expenses (`expenses`)
-- Stores operational business expenses (Rent, Electricity, Salaries, Transport, etc.)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.expenses (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT REFERENCES public.businesses(id) ON DELETE CASCADE,
    category TEXT DEFAULT 'Rent',     -- 'Rent', 'Electricity', 'Salaries', 'Transport', 'Packaging', 'Tea/Snacks', 'Marketing', 'Other'
    amount NUMERIC(12, 2) NOT NULL,
    payment_mode TEXT DEFAULT 'CASH', -- 'CASH', 'UPI', 'BANK_TRANSFER', 'CARD'
    paid_to TEXT DEFAULT '',
    vendor_name TEXT DEFAULT '',      -- backward-compatibility alias
    notes TEXT DEFAULT '',
    date BIGINT NOT NULL,             -- Epoch timestamp in ms
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ==============================================================================
-- 8. Performance Indexes
-- Speeds up queries on business IDs, invoices, dates, and party lookups
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_businesses_user_id ON public.businesses(user_id);
CREATE INDEX IF NOT EXISTS idx_items_business_id ON public.items(business_id);
CREATE INDEX IF NOT EXISTS idx_parties_business_id ON public.parties(business_id);
CREATE INDEX IF NOT EXISTS idx_invoices_business_id ON public.invoices(business_id);
CREATE INDEX IF NOT EXISTS idx_invoices_invoice_number ON public.invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_invoices_party_id ON public.invoices(party_id);
CREATE INDEX IF NOT EXISTS idx_invoices_date ON public.invoices(date);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_id ON public.invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payments_business_id ON public.payments(business_id);
CREATE INDEX IF NOT EXISTS idx_payments_invoice_id ON public.payments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_expenses_business_id ON public.expenses(business_id);

-- ==============================================================================
-- 9. Row Level Security (RLS)
-- Enables secure access control for mobile app requests
-- ==============================================================================
ALTER TABLE public.businesses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.parties ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.invoice_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;

-- Allow full access with publishable/anon key or authenticated user
-- Drop existing policies if they already exist to allow clean re-runs
DROP POLICY IF EXISTS "Allow authenticated and anon access to businesses" ON public.businesses;
CREATE POLICY "Allow authenticated and anon access to businesses" ON public.businesses
    FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow authenticated and anon access to items" ON public.items;
CREATE POLICY "Allow authenticated and anon access to items" ON public.items
    FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow authenticated and anon access to parties" ON public.parties;
CREATE POLICY "Allow authenticated and anon access to parties" ON public.parties
    FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow authenticated and anon access to invoices" ON public.invoices;
CREATE POLICY "Allow authenticated and anon access to invoices" ON public.invoices
    FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow authenticated and anon access to invoice_items" ON public.invoice_items;
CREATE POLICY "Allow authenticated and anon access to invoice_items" ON public.invoice_items
    FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow authenticated and anon access to payments" ON public.payments;
CREATE POLICY "Allow authenticated and anon access to payments" ON public.payments
    FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow authenticated and anon access to expenses" ON public.expenses;
CREATE POLICY "Allow authenticated and anon access to expenses" ON public.expenses
    FOR ALL USING (true) WITH CHECK (true);
