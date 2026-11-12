/* PharmaERP single-page frontend. Vanilla JS, no build step. */

const API = '/api';

/* ---------- small helpers ---------- */
const $ = (sel, root = document) => root.querySelector(sel);
const el = (tag, props = {}, children = []) => {
  const node = document.createElement(tag);
  Object.entries(props).forEach(([k, v]) => {
    if (k === 'class') node.className = v;
    else if (k === 'html') node.innerHTML = v;
    else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2), v);
    else if (v !== null && v !== undefined) node.setAttribute(k, v);
  });
  (Array.isArray(children) ? children : [children]).forEach(c => {
    if (c == null) return;
    node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
  });
  return node;
};
const money = n => (n == null ? '0.00' : Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }));
/** Rupee-formatted amount, e.g. ₹ 49,477.00 */
const rupee = n => '\u20B9 ' + money(n);
const fmtDate = d => d ? d : '—';
/** DD/MM/YY for the printed bill. */
const shortDate = iso => {
  if (!iso) return '';
  const [y, m, d] = iso.split('-');
  return (d || '') + '/' + (m || '') + '/' + (y ? y.slice(2) : '');
};
/** MM/YY for expiry columns. */
const expMMYY = iso => {
  if (!iso) return '';
  const [y, m] = iso.split('-');
  return (m || '') + '/' + (y ? y.slice(2) : '');
};
const esc = s => String(s ?? '').replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));

/** Company profile cache (loaded at boot). */
let COMPANY = null;

async function api(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || ('Request failed (' + res.status + ')'));
  return data;
}

function toast(message, type = '') {
  const t = el('div', { class: 'toast ' + type }, message);
  $('#toastRoot').appendChild(t);
  setTimeout(() => t.remove(), 3200);
}

/* ---------- modal ---------- */
function openModal(title, bodyNode, { onSave, saveLabel = 'Save', wide = false } = {}) {
  closeModal();
  const overlay = el('div', { class: 'modal-overlay' });
  const modal = el('div', { class: 'modal' });
  if (wide) modal.style.maxWidth = '820px';
  const foot = el('div', { class: 'modal-foot' }, [
    el('button', { class: 'btn', onclick: closeModal }, 'Cancel'),
  ]);
  if (onSave) {
    foot.appendChild(el('button', { class: 'btn primary', onclick: async () => {
      try { await onSave(); } catch (e) { toast(e.message, 'error'); }
    } }, saveLabel));
  }
  modal.append(
    el('div', { class: 'modal-head' }, [
      el('h3', {}, title),
      el('button', { class: 'close-x', onclick: closeModal }, '\u00d7'),
    ]),
    el('div', { class: 'modal-body' }, bodyNode),
    foot
  );
  overlay.appendChild(modal);
  overlay.addEventListener('click', e => { if (e.target === overlay) closeModal(); });
  $('#modalRoot').appendChild(overlay);
}
function closeModal() { $('#modalRoot').innerHTML = ''; }

function field(label, inputNode, full = false) {
  return el('div', { class: 'field' + (full ? ' full' : '') }, [
    el('label', {}, label), inputNode,
  ]);
}
function input(name, value = '', attrs = {}) {
  return el('input', { name, value: value ?? '', ...attrs });
}

function confirmDelete(message, onYes) {
  openModal('Please confirm', el('p', {}, message), {
    saveLabel: 'Delete',
    onSave: async () => { await onYes(); closeModal(); },
  });
  const btn = $('#modalRoot .btn.primary');
  if (btn) { btn.classList.remove('primary'); btn.classList.add('danger'); }
}

/* ---------- table helper ---------- */
function table(columns, rows, renderRow) {
  if (!rows.length) return el('div', { class: 'empty' }, 'No records yet.');
  const thead = el('thead', {}, el('tr', {}, columns.map(c =>
    el('th', { class: c.num ? 'num' : '' }, c.label))));
  const tbody = el('tbody', {}, rows.map(renderRow));
  return el('div', { class: 'table-wrap' }, el('table', {}, [thead, tbody]));
}

/* ---------- views ---------- */
const state = { view: 'dashboard' };

const views = {
  dashboard: renderDashboard,
  products: renderProducts,
  inventory: renderInventory,
  purchases: renderPurchases,
  sales: renderSales,
  suppliers: renderSuppliers,
  customers: renderCustomers,
  settings: renderSettings,
};
const titles = {
  dashboard: 'Dashboard', products: 'Products', inventory: 'Inventory',
  purchases: 'Purchase Orders', sales: 'Sales / Invoices',
  suppliers: 'Suppliers', customers: 'Customers', settings: 'Company / Settings',
};

async function navigate(view) {
  state.view = view;
  $('#viewTitle').textContent = titles[view];
  $('#topbarActions').innerHTML = '';
  document.querySelectorAll('.nav-item').forEach(b =>
    b.classList.toggle('active', b.dataset.view === view));
  const container = $('#view');
  container.innerHTML = '<div class="empty">Loading…</div>';
  try {
    await views[view](container);
  } catch (e) {
    container.innerHTML = '';
    container.appendChild(el('div', { class: 'empty' }, 'Error: ' + e.message));
  }
}

/* ===== Dashboard ===== */
async function renderDashboard(c) {
  const s = await api('/dashboard/stats');
  c.innerHTML = '';
  const kpi = (label, value, sub, cls = '') =>
    el('div', { class: 'kpi ' + cls }, [
      el('div', { class: 'label' }, label),
      el('div', { class: 'value' }, String(value)),
      sub ? el('div', { class: 'sub' }, sub) : null,
    ]);
  c.appendChild(el('div', { class: 'kpi-grid' }, [
    kpi('Products', s.totalProducts, 'Active SKUs'),
    kpi('Inventory value', rupee(s.inventoryValue), s.totalStockUnits + ' units in stock'),
    kpi('Low stock', s.lowStockCount, 'At or below reorder level', s.lowStockCount ? 'warn' : 'ok'),
    kpi('Expired batches', s.expiredBatchCount, 'Remove from sellable stock', s.expiredBatchCount ? 'danger' : 'ok'),
    kpi('Expiring (90d)', s.expiringSoonCount, 'Batches nearing expiry', s.expiringSoonCount ? 'warn' : 'ok'),
    kpi('Revenue (MTD)', rupee(s.salesRevenueThisMonth), s.confirmedSalesThisMonth + ' confirmed invoices', 'ok'),
    kpi('Open POs', s.openPurchaseOrders, 'Draft purchase orders'),
    kpi('Partners', (s.totalSuppliers + s.totalCustomers), s.totalSuppliers + ' suppliers · ' + s.totalCustomers + ' customers'),
  ]));

  const [expired, expiring, summary] = await Promise.all([
    api('/inventory/expired'),
    api('/inventory/expiring-soon?days=90'),
    api('/inventory/summary'),
  ]);

  const low = summary.filter(x => x.lowStock);
  c.appendChild(card('Low stock items', table(
    [{ label: 'Product' }, { label: 'In stock', num: true }, { label: 'Reorder level', num: true }],
    low,
    r => el('tr', {}, [
      el('td', {}, r.name),
      el('td', { class: 'num' }, String(r.totalQuantity)),
      el('td', { class: 'num' }, String(r.reorderLevel)),
    ])
  )));

  const alerts = [...expired.map(b => ({ b, kind: 'expired' })), ...expiring.map(b => ({ b, kind: 'soon' }))];
  c.appendChild(card('Expiry alerts', table(
    [{ label: 'Product' }, { label: 'Lot' }, { label: 'Expiry' }, { label: 'Qty', num: true }, { label: 'Status' }],
    alerts,
    ({ b, kind }) => el('tr', {}, [
      el('td', {}, b.product ? b.product.name : '—'),
      el('td', {}, b.lotNumber || '—'),
      el('td', {}, fmtDate(b.expiryDate)),
      el('td', { class: 'num' }, String(b.quantity)),
      el('td', {}, el('span', { class: 'badge ' + (kind === 'expired' ? 'red' : 'amber') },
        kind === 'expired' ? 'Expired' : 'Expiring soon')),
    ])
  )));
}

function card(title, bodyNode) {
  return el('div', { class: 'section-card' }, [el('h2', {}, title), bodyNode]);
}

/* ===== Products ===== */
async function renderProducts(c) {
  addTopButton('+ New Product', () => productForm());
  const products = await api('/products');
  c.innerHTML = '';
  const search = input('q', '', { placeholder: 'Search by name, generic or SKU…', class: '' });
  const toolbar = el('div', { class: 'toolbar' }, [
    el('div', { class: 'search' }, search),
  ]);
  const listWrap = el('div', {});
  const draw = (items) => {
    listWrap.innerHTML = '';
    listWrap.appendChild(card('All products', table(
      [{ label: 'Name' }, { label: 'HSN' }, { label: 'Pack' }, { label: 'GST%', num: true },
       { label: 'Rx' }, { label: 'Purchase', num: true }, { label: 'Rate', num: true },
       { label: 'MRP', num: true }, { label: 'Reorder', num: true }, { label: '' }],
      items,
      p => el('tr', {}, [
        el('td', {}, p.name),
        el('td', {}, p.hsnCode || '—'),
        el('td', {}, p.pack || '—'),
        el('td', { class: 'num' }, money(p.gstRate) + '%'),
        el('td', {}, p.prescriptionRequired ? el('span', { class: 'badge blue' }, 'Rx') : el('span', { class: 'badge gray' }, 'OTC')),
        el('td', { class: 'num' }, rupee(p.purchasePrice)),
        el('td', { class: 'num' }, rupee(p.sellingPrice)),
        el('td', { class: 'num' }, rupee(p.mrp)),
        el('td', { class: 'num' }, String(p.reorderLevel)),
        el('td', {}, el('div', { class: 'actions-cell' }, [
          el('button', { class: 'btn small', onclick: () => productForm(p) }, 'Edit'),
          el('button', { class: 'btn small danger', onclick: () => confirmDelete(
            'Delete product "' + p.name + '"?', async () => {
              await api('/products/' + p.id, { method: 'DELETE' });
              toast('Product deleted', 'ok'); navigate('products');
            }) }, 'Delete'),
        ])),
      ])
    )));
  };
  draw(products);
  let timer;
  search.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(async () => {
      const q = search.value.trim();
      const items = q ? await api('/products?q=' + encodeURIComponent(q)) : products;
      draw(items);
    }, 250);
  });
  c.append(toolbar, listWrap);
}

function productForm(p = null) {
  const b = {};
  const form = el('div', { class: 'form-grid' }, [
    field('Name *', b.name = input('name', p?.name)),
    field('HSN code', b.hsnCode = input('hsnCode', p?.hsnCode, { placeholder: '30049099' })),
    field('Generic name', b.genericName = input('genericName', p?.genericName)),
    field('Manufacturer', b.manufacturer = input('manufacturer', p?.manufacturer)),
    field('Form', b.form = input('form', p?.form, { placeholder: 'Tablet, Syrup…' })),
    field('Pack', b.pack = input('pack', p?.pack, { placeholder: '500ML, 10 Tab' })),
    field('SKU', b.sku = input('sku', p?.sku)),
    field('Category', b.category = input('category', p?.category)),
    field('GST rate (%)', b.gstRate = input('gstRate', p?.gstRate ?? 5, { type: 'number', step: '0.01', min: 0 })),
    field('MRP', b.mrp = input('mrp', p?.mrp ?? 0, { type: 'number', step: '0.01', min: 0 })),
    field('Purchase price', b.purchasePrice = input('purchasePrice', p?.purchasePrice ?? 0, { type: 'number', step: '0.01', min: 0 })),
    field('Rate / Selling price', b.sellingPrice = input('sellingPrice', p?.sellingPrice ?? 0, { type: 'number', step: '0.01', min: 0 })),
    field('Reorder level', b.reorderLevel = input('reorderLevel', p?.reorderLevel ?? 10, { type: 'number', min: 0 })),
    el('div', { class: 'field check' }, [
      b.prescriptionRequired = el('input', { type: 'checkbox', ...(p?.prescriptionRequired ? { checked: 'checked' } : {}) }),
      el('label', { style: 'margin:0' }, 'Prescription required (Rx)'),
    ]),
  ]);
  openModal(p ? 'Edit product' : 'New product', form, {
    saveLabel: p ? 'Update' : 'Create',
    onSave: async () => {
      const payload = {
        name: b.name.value.trim(),
        sku: b.sku.value.trim() || null,
        hsnCode: b.hsnCode.value.trim() || null,
        pack: b.pack.value.trim() || null,
        genericName: b.genericName.value.trim() || null,
        manufacturer: b.manufacturer.value.trim() || null,
        form: b.form.value.trim() || null,
        category: b.category.value.trim() || null,
        gstRate: Number(b.gstRate.value || 0),
        mrp: Number(b.mrp.value || 0),
        reorderLevel: Number(b.reorderLevel.value || 0),
        purchasePrice: Number(b.purchasePrice.value || 0),
        sellingPrice: Number(b.sellingPrice.value || 0),
        prescriptionRequired: b.prescriptionRequired.checked,
      };
      if (!payload.name) throw new Error('Name is required');
      if (p) await api('/products/' + p.id, { method: 'PUT', body: JSON.stringify(payload) });
      else await api('/products', { method: 'POST', body: JSON.stringify(payload) });
      toast(p ? 'Product updated' : 'Product created', 'ok');
      closeModal(); navigate('products');
    },
  });
}

/* ===== Inventory ===== */
async function renderInventory(c) {
  addTopButton('+ Stock receipt', () => stockAdjustForm());
  const [summary, batches] = await Promise.all([
    api('/inventory/summary'),
    api('/inventory/batches'),
  ]);
  c.innerHTML = '';
  c.appendChild(card('Stock by product', table(
    [{ label: 'Product' }, { label: 'SKU' }, { label: 'Form' }, { label: 'In stock', num: true },
     { label: 'Reorder', num: true }, { label: 'Earliest expiry' }, { label: 'Stock value', num: true }, { label: 'Status' }],
    summary,
    r => el('tr', {}, [
      el('td', {}, r.name),
      el('td', {}, r.sku || '—'),
      el('td', {}, r.form || '—'),
      el('td', { class: 'num' }, String(r.totalQuantity)),
      el('td', { class: 'num' }, String(r.reorderLevel)),
      el('td', {}, fmtDate(r.earliestExpiry)),
      el('td', { class: 'num' }, rupee(r.stockValue)),
      el('td', {}, r.lowStock
        ? el('span', { class: 'badge amber' }, 'Low')
        : el('span', { class: 'badge green' }, 'OK')),
    ])
  )));

  const today = new Date().toISOString().slice(0, 10);
  c.appendChild(card('Batches / lots', table(
    [{ label: 'Product' }, { label: 'Lot' }, { label: 'Mfg date' }, { label: 'Expiry' },
     { label: 'Qty', num: true }, { label: 'Status' }, { label: '' }],
    batches,
    bch => {
      const expired = bch.expiryDate && bch.expiryDate < today;
      return el('tr', {}, [
        el('td', {}, bch.product ? bch.product.name : '—'),
        el('td', {}, bch.lotNumber || '—'),
        el('td', {}, fmtDate(bch.manufactureDate)),
        el('td', {}, fmtDate(bch.expiryDate)),
        el('td', { class: 'num' }, String(bch.quantity)),
        el('td', {}, expired
          ? el('span', { class: 'badge red' }, 'Expired')
          : el('span', { class: 'badge green' }, 'Valid')),
        el('td', {}, el('button', { class: 'btn small danger', onclick: () => confirmDelete(
          'Delete batch "' + (bch.lotNumber || bch.id) + '"? This removes its stock.',
          async () => { await api('/inventory/batches/' + bch.id, { method: 'DELETE' });
            toast('Batch removed', 'ok'); navigate('inventory'); }) }, 'Delete')),
      ]);
    }
  )));
}

async function stockAdjustForm() {
  const products = await api('/products');
  const b = {};
  const sel = el('select', { name: 'productId' }, products.map(p =>
    el('option', { value: p.id }, p.name + (p.strength ? ' (' + p.strength + ')' : ''))));
  const form = el('div', { class: 'form-grid' }, [
    field('Product *', b.product = sel, true),
    field('Lot number', b.lot = input('lotNumber', '', { placeholder: 'e.g. PARA-A25' })),
    field('Expiry date', b.expiry = input('expiryDate', '', { type: 'date' })),
    field('Quantity to add *', b.qty = input('quantity', 1, { type: 'number', min: 1 })),
  ]);
  openModal('Stock receipt / adjustment', form, {
    saveLabel: 'Add stock',
    onSave: async () => {
      const payload = {
        productId: Number(b.product.value),
        lotNumber: b.lot.value.trim() || null,
        expiryDate: b.expiry.value || null,
        quantity: Number(b.qty.value || 0),
      };
      if (payload.quantity <= 0) throw new Error('Quantity must be positive');
      await api('/inventory/adjust', { method: 'POST', body: JSON.stringify(payload) });
      toast('Stock added', 'ok'); closeModal(); navigate('inventory');
    },
  });
}

/* ===== Purchase Orders ===== */
async function renderPurchases(c) {
  addTopButton('+ New Purchase Order', () => orderForm('purchase'));
  const orders = await api('/purchase-orders');
  c.innerHTML = '';
  c.appendChild(card('Purchase orders', table(
    [{ label: 'PO #' }, { label: 'Supplier' }, { label: 'Date' }, { label: 'Items', num: true },
     { label: 'Total', num: true }, { label: 'Status' }, { label: '' }],
    orders,
    o => el('tr', {}, [
      el('td', {}, o.orderNumber || ('#' + o.id)),
      el('td', {}, o.supplier ? o.supplier.name : '—'),
      el('td', {}, fmtDate(o.orderDate)),
      el('td', { class: 'num' }, String(o.items ? o.items.length : 0)),
      el('td', { class: 'num' }, rupee(o.total)),
      el('td', {}, statusBadge(o.status)),
      el('td', {}, el('div', { class: 'actions-cell' }, orderActions('purchase', o))),
    ])
  )));
}

/* ===== Sales Orders ===== */
async function renderSales(c) {
  addTopButton('+ New Sales Invoice', () => orderForm('sales'));
  const orders = await api('/sales-orders');
  c.innerHTML = '';
  c.appendChild(card('Sales orders / invoices', table(
    [{ label: 'Invoice #' }, { label: 'Customer' }, { label: 'Date' }, { label: 'Items', num: true },
     { label: 'Total', num: true }, { label: 'Status' }, { label: '' }],
    orders,
    o => el('tr', {}, [
      el('td', {}, o.invoiceNumber || ('#' + o.id)),
      el('td', {}, o.customer ? o.customer.name : '—'),
      el('td', {}, fmtDate(o.orderDate)),
      el('td', { class: 'num' }, String(o.items ? o.items.length : 0)),
      el('td', { class: 'num' }, rupee(o.total)),
      el('td', {}, statusBadge(o.status)),
      el('td', {}, el('div', { class: 'actions-cell' }, orderActions('sales', o))),
    ])
  )));
}

function statusBadge(status) {
  const map = { DRAFT: 'gray', RECEIVED: 'green', CONFIRMED: 'green', CANCELLED: 'red' };
  return el('span', { class: 'badge ' + (map[status] || 'gray') }, status);
}

function orderActions(kind, o) {
  const base = kind === 'purchase' ? '/purchase-orders' : '/sales-orders';
  const act = kind === 'purchase' ? 'receive' : 'confirm';
  const actLabel = kind === 'purchase' ? 'Receive' : 'Confirm';
  const btns = [];
  if (o.status === 'DRAFT') {
    btns.push(el('button', { class: 'btn small primary', onclick: async () => {
      try { await api(base + '/' + o.id + '/' + act, { method: 'POST' });
        toast(actLabel + 'd', 'ok'); navigate(kind === 'purchase' ? 'purchases' : 'sales'); }
      catch (e) { toast(e.message, 'error'); }
    } }, actLabel));
    btns.push(el('button', { class: 'btn small', onclick: async () => {
      try { await api(base + '/' + o.id + '/cancel', { method: 'POST' });
        toast('Cancelled', 'ok'); navigate(kind === 'purchase' ? 'purchases' : 'sales'); }
      catch (e) { toast(e.message, 'error'); }
    } }, 'Cancel'));
    btns.push(el('button', { class: 'btn small danger', onclick: () => confirmDelete(
      'Delete this order?', async () => { await api(base + '/' + o.id, { method: 'DELETE' });
        toast('Deleted', 'ok'); navigate(kind === 'purchase' ? 'purchases' : 'sales'); }) }, 'Delete'));
  } else {
    btns.push(el('button', { class: 'btn small', onclick: () => orderDetails(kind, o) }, 'View'));
  }
  if (kind === 'sales') {
    btns.push(el('button', { class: 'btn small primary', onclick: () => printInvoice(o.id) }, 'Print Bill'));
  }
  return btns;
}

/* ---------- printable GST bill ---------- */
async function printInvoice(id) {
  let data;
  try {
    data = await api('/sales-orders/' + id + '/invoice');
  } catch (e) { toast(e.message, 'error'); return; }

  if (!data.company || !data.company.configured) {
    toast('Set up your company details first (Settings)', 'error');
    companySetupForm(false);
    return;
  }

  const html = buildInvoiceHtml(data);
  const w = window.open('', '_blank', 'width=1100,height=800');
  if (!w) { toast('Pop-up blocked. Allow pop-ups to print bills.', 'error'); return; }
  w.document.open();
  w.document.write(html);
  w.document.close();
  w.focus();
  setTimeout(() => w.print(), 400);
}

function buildInvoiceHtml(d) {
  const co = d.company;
  const cust = d.customer || {};
  const addr = [co.addressLine1, co.addressLine2,
    [co.city, co.state, co.pincode].filter(Boolean).join(', ')].filter(Boolean).map(esc).join('<br>');
  const custAddr = [cust.address, [cust.phone].filter(Boolean).join('')].filter(Boolean).map(esc).join('<br>');

  const itemRows = (d.items || []).map((it, i) => `
    <tr>
      <td class="c">${i + 1}</td>
      <td>${esc(it.hsnCode || (it.product && it.product.hsnCode) || '')}</td>
      <td class="nm">${esc(it.product ? it.product.name : '')}</td>
      <td class="c">${esc(it.pack || (it.product && it.product.pack) || '')}</td>
      <td class="c">${esc(it.batchNumber || '')}</td>
      <td class="c">${esc(it.expiryDate || '')}</td>
      <td class="r">${it.quantity}</td>
      <td class="r">${it.freeQuantity || ''}</td>
      <td class="r">${money(it.unitPrice)}</td>
      <td class="r">${money(it.mrp)}</td>
      <td class="r">${money(it.discountPercent)}</td>
      <td class="r">${money((Number(it.gstRate) || 0) / 2)}</td>
      <td class="r">${money((Number(it.gstRate) || 0) / 2)}</td>
      <td class="r">${money(it.lineTotal)}</td>
    </tr>`).join('');

  // Fixed class rows like the sample (0/5/12/18/28).
  const classRates = ['0', '5', '12', '18', '28'];
  const byRate = {};
  (d.gstClasses || []).forEach(g => { byRate[Number(g.rate).toString()] = g; });
  const classRows = classRates.map(rate => {
    const g = byRate[rate];
    return `<tr>
      <td>${rate}%</td>
      <td class="r">${g ? money(g.taxable) : ''}</td>
      <td class="r">${g ? money(g.discount) : ''}</td>
      <td class="r">${g ? money(g.sgst) : ''}</td>
      <td class="r">${g ? money(g.cgst) : ''}</td>
      <td class="r">${g ? money(g.totalGst) : ''}</td>
    </tr>`;
  }).join('');

  const terms = esc(co.termsAndConditions || '').replace(/\n/g, '<br>');
  const bankLine = 'Bank : ' + esc(co.bankName || '') +
    ' | Branch : ' + esc(co.bankBranch || '') +
    ' | A/C No : ' + esc(co.bankAccountNumber || '') +
    ' | I.F.S.C. : ' + esc(co.bankIfsc || '') +
    ' | Upi Id: ' + esc(co.upiId || '');

  return `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${esc(d.invoiceNumber)}</title>
  <style>
    /* Print on the top half of an A4 sheet: A5 landscape == half of A4 portrait. */
    @page { size: A5 landscape; margin: 4mm; }
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; }
    body { font-family: Arial, Helvetica, sans-serif; font-size: 8px; color: #000; }
    /* On screen, show the bill on a half-A4 canvas so preview matches print. */
    .bill {
      border: 1px solid #000;
      width: 210mm;            /* A4 width -> A5 landscape width */
      max-width: 100%;
      margin: 6mm auto;
    }
    .row { display: flex; }
    .head { display: flex; border-bottom: 1px solid #000; }
    .head > div { flex: 1; padding: 3px 5px; }
    .head .seller h1 { margin: 0; font-size: 13px; line-height: 1.1; }
    .head .buyer { border-left: 1px solid #000; }
    .head .buyer h3 { font-size: 10px; }
    .head small { display: block; line-height: 1.25; font-size: 7.5px; }
    .meta { display: flex; border-bottom: 1px solid #000; }
    .meta .lic { flex: 2; padding: 2px 5px; border-right: 1px solid #000; }
    .meta .lic small { display: block; line-height: 1.25; }
    .meta .title { flex: 1; text-align: center; padding: 2px 5px; border-right: 1px solid #000; }
    .meta .title b { font-size: 11px; }
    .meta .inv { flex: 1.2; padding: 2px 5px; }
    .meta .inv small { display: block; line-height: 1.3; }
    table { width: 100%; border-collapse: collapse; }
    .items th, .items td { border: 1px solid #000; padding: 1px 3px; font-size: 7.5px; line-height: 1.2; }
    .items th { background: #f0f0f0; }
    .items .nm { min-width: 110px; text-align: left; }
    .r { text-align: right; } .c { text-align: center; }
    .summary { display: flex; border-top: 1px solid #000; }
    .summary .classes { flex: 1.6; border-right: 1px solid #000; }
    .summary .totals { flex: 1; }
    .summary table td, .summary table th { border: 1px solid #000; padding: 1px 4px; font-size: 7.5px; }
    .totals td:first-child { font-weight: bold; }
    .foot { display: flex; border-top: 1px solid #000; }
    .foot .terms { flex: 1.6; padding: 3px 5px; border-right: 1px solid #000; font-size: 7.5px; }
    .foot .sign { flex: 1; padding: 3px 5px; text-align: center; font-size: 8px; }
    .grand { display: flex; justify-content: space-between; border-top: 1px solid #000; padding: 3px 6px; font-size: 11px; font-weight: bold; }
    .bank { border-top: 1px solid #000; padding: 2px 5px; font-weight: bold; font-size: 7.5px; }
    h1,h2,h3 { margin: 0; }
    @media print {
      html, body { width: 210mm; }
      .bill { width: 100%; margin: 0; border: 1px solid #000; }
      /* Keep the whole bill on a single half-A4 page. */
      .bill { page-break-inside: avoid; }
    }
  </style></head><body>
  <div class="bill">
    <div class="head">
      <div class="seller">
        <h1>${esc(co.companyName || '')}</h1>
        <small>${addr}</small>
        <small>Phone : ${esc(co.phone || '')}</small>
        <small>Email : ${esc(co.email || '')}</small>
      </div>
      <div class="buyer">
        <h3>${esc(cust.name || 'Cash / Walk-in')}</h3>
        <small>${custAddr}</small>
        <small>GST : ${esc(cust.gstNumber || '')}</small>
      </div>
    </div>
    <div class="meta">
      <div class="lic">
        <small>D.L. No : ${esc(co.dlNumber || '')}</small>
        <small>GST No : ${esc(co.gstNumber || '')}</small>
        <small>FSSAI No : ${esc(co.fssaiNumber || '')}</small>
      </div>
      <div class="title"><b>GST INVOICE</b><br><small>${esc(d.status || '')}</small></div>
      <div class="inv">
        <small>Invoice No : <b>${esc(d.invoiceNumber)}</b></small>
        <small>Date : <b>${shortDate(d.orderDate)}</b></small>
        <small>Due Date : <b>${shortDate(d.dueDate)}</b></small>
      </div>
    </div>
    <table class="items">
      <thead><tr>
        <th>Sn</th><th>HSN</th><th>ITEM NAME</th><th>PACK</th><th>BATCH</th><th>EXP</th>
        <th>QTY</th><th>FREE</th><th>RATE</th><th>MRP</th><th>DISC</th><th>SGST</th><th>CGST</th><th>AMOUNT</th>
      </tr></thead>
      <tbody>${itemRows}</tbody>
    </table>
    <div class="summary">
      <div class="classes">
        <table>
          <thead><tr><th>CLASS</th><th class="r">TOTAL</th><th class="r">DISC</th><th class="r">SGST</th><th class="r">CGST</th><th class="r">TOTAL GST</th></tr></thead>
          <tbody>${classRows}
            <tr><td><b>TOTAL</b></td><td class="r"><b>${money(d.subTotal)}</b></td><td class="r"><b>${money(d.itemDiscount)}</b></td>
            <td class="r"><b>${money(d.totalSgst)}</b></td><td class="r"><b>${money(d.totalCgst)}</b></td><td class="r"><b>${money(d.totalGst)}</b></td></tr>
          </tbody>
        </table>
      </div>
      <div class="totals">
        <table>
          <tr><td>SUB TOTAL</td><td class="r">${rupee(d.subTotal)}</td></tr>
          <tr><td>ITEM DISCOUNT</td><td class="r">${rupee(d.itemDiscount)}</td></tr>
          <tr><td>GST PAYABLE</td><td class="r">${rupee(d.totalGst)}</td></tr>
          <tr><td>ROUND OFF</td><td class="r">${rupee(d.roundOff)}</td></tr>
          <tr><td>NO OF ITEM</td><td class="r">${d.itemCount}</td></tr>
        </table>
      </div>
    </div>
    <div class="grand"><span>GRAND TOTAL</span><span>${rupee(d.grandTotal)}</span></div>
    <div class="foot">
      <div class="terms"><b>Terms &amp; Conditions :-</b><br>${terms}</div>
      <div class="sign">For ${esc(co.companyName || '')}<br><br><br>Auth. Sign</div>
    </div>
    <div class="bank">${bankLine}</div>
  </div>
  </body></html>`;
}

function orderDetails(kind, o) {
  const partner = kind === 'purchase' ? (o.supplier?.name || '—') : (o.customer?.name || '—');
  const number = kind === 'purchase' ? o.orderNumber : o.invoiceNumber;
  const rows = (o.items || []).map(it => el('tr', {}, [
    el('td', {}, it.product ? it.product.name : '—'),
    el('td', { class: 'num' }, String(it.quantity)),
    el('td', { class: 'num' }, rupee(it.unitPrice)),
    el('td', { class: 'num' }, rupee((it.unitPrice || 0) * it.quantity)),
  ]));
  const body = el('div', {}, [
    el('p', {}, [el('strong', {}, number || ('#' + o.id)), ' — ' + partner + ' — ' + fmtDate(o.orderDate)]),
    el('div', { class: 'table-wrap' }, el('table', {}, [
      el('thead', {}, el('tr', {}, [el('th', {}, 'Product'), el('th', { class: 'num' }, 'Qty'),
        el('th', { class: 'num' }, 'Unit'), el('th', { class: 'num' }, 'Total')])),
      el('tbody', {}, rows),
    ])),
    el('p', { class: 'right' }, [el('strong', {}, 'Total: ' + rupee(o.total))]),
  ]);
  openModal((kind === 'purchase' ? 'Purchase order ' : 'Invoice ') + (number || ''), body, {});
}

async function orderForm(kind) {
  const [products, partners] = await Promise.all([
    api('/products'),
    api(kind === 'purchase' ? '/suppliers' : '/customers'),
  ]);
  const isPurchase = kind === 'purchase';
  const productById = Object.fromEntries(products.map(p => [String(p.id), p]));

  const partnerSel = el('select', {}, [
    el('option', { value: '' }, isPurchase ? '— none —' : 'Walk-in / Cash'),
    ...partners.map(p => el('option', { value: p.id }, p.name + (p.type === 'WALK_IN' ? ' (walk-in)' : ''))),
  ]);

  // For sales, allow adding a walk-in customer inline and auto-selecting it.
  const walkInBtn = isPurchase ? null : el('button', { class: 'btn small', type: 'button',
    onclick: () => walkInCustomerForm((created) => {
      partnerSel.appendChild(el('option', { value: created.id, selected: 'selected' },
        created.name + ' (walk-in)'));
      partnerSel.value = String(created.id);
    }) }, '+ Walk-in');

  const lineBody = el('tbody', {});
  const lineRows = []; // keep structured refs to each row's inputs

  const addLine = () => {
    const psel = el('select', {}, products.map(p =>
      el('option', { value: p.id }, p.name + (p.pack ? ' (' + p.pack + ')' : ''))));
    const priceOf = p => isPurchase ? p.purchasePrice : p.sellingPrice;
    const first = products[0];

    const qty = el('input', { type: 'number', min: 0, value: 1, style: 'width:70px' });
    const price = el('input', { type: 'number', min: 0, step: '0.01', value: first ? priceOf(first) : 0, style: 'width:90px' });

    let ref;
    if (isPurchase) {
      const lot = el('input', { placeholder: 'Lot', style: 'width:90px' });
      const exp = el('input', { type: 'date', style: 'width:140px' });
      ref = { psel, lot, exp, qty, price };
      psel.addEventListener('change', () => { price.value = priceOf(productById[psel.value]) ?? 0; });
      const row = el('tr', {}, [
        el('td', {}, psel), el('td', {}, lot), el('td', {}, exp),
        el('td', {}, qty), el('td', {}, price),
        el('td', {}, el('button', { class: 'btn small danger', onclick: () => { row.remove(); ref.removed = true; } }, '\u00d7')),
      ]);
      lineBody.appendChild(row);
    } else {
      const batch = el('input', { placeholder: 'Batch', style: 'width:90px' });
      const exp = el('input', { placeholder: 'MM/YY', style: 'width:70px' });
      const free = el('input', { type: 'number', min: 0, value: 0, style: 'width:60px' });
      const gst = el('input', { type: 'number', min: 0, step: '0.01', value: first ? first.gstRate : 5, style: 'width:70px' });
      const disc = el('input', { type: 'number', min: 0, step: '0.01', value: 0, style: 'width:70px' });
      ref = { psel, batch, exp, qty, free, price, disc, gst };
      psel.addEventListener('change', () => {
        const p = productById[psel.value];
        price.value = priceOf(p) ?? 0;
        gst.value = p?.gstRate ?? 0;
      });
      const row = el('tr', {}, [
        el('td', {}, psel), el('td', {}, batch), el('td', {}, exp),
        el('td', {}, qty), el('td', {}, free), el('td', {}, price),
        el('td', {}, disc), el('td', {}, gst),
        el('td', {}, el('button', { class: 'btn small danger', onclick: () => { row.remove(); ref.removed = true; } }, '\u00d7')),
      ]);
      lineBody.appendChild(row);
    }
    lineRows.push(ref);
  };

  const headCols = isPurchase
    ? ['Product', 'Lot', 'Expiry', 'Qty', 'Unit price', '']
    : ['Product', 'Batch', 'Exp', 'Qty', 'Free', 'Rate', 'Disc%', 'GST%', ''];

  const lineTable = el('div', { class: 'line-items table-wrap' }, el('table', {}, [
    el('thead', {}, el('tr', {}, headCols.map(h => el('th', {}, h)))),
    lineBody,
  ]));
  addLine();

  const partnerField = isPurchase
    ? field('Supplier', partnerSel, true)
    : el('div', { class: 'field full' }, [
        el('label', {}, 'Customer'),
        el('div', { style: 'display:flex; gap:8px; align-items:center' }, [partnerSel, walkInBtn]),
      ]);

  const body = el('div', {}, [
    el('div', { class: 'form-grid' }, [partnerField]),
    lineTable,
    el('button', { class: 'btn small line-add', onclick: addLine }, '+ Add line'),
  ]);

  openModal(isPurchase ? 'New purchase order' : 'New sales invoice', body, {
    wide: true,
    saveLabel: 'Create',
    onSave: async () => {
      const items = lineRows.filter(r => !r.removed).map(r => {
        const productId = Number(r.psel.value);
        if (isPurchase) {
          return {
            product: { id: productId },
            lotNumber: r.lot.value.trim() || null,
            expiryDate: r.exp.value || null,
            quantity: Number(r.qty.value || 0),
            unitPrice: Number(r.price.value || 0),
          };
        }
        return {
          product: { id: productId },
          batchNumber: r.batch.value.trim() || null,
          expiryDate: r.exp.value.trim() || null,
          quantity: Number(r.qty.value || 0),
          freeQuantity: Number(r.free.value || 0),
          unitPrice: Number(r.price.value || 0),
          discountPercent: Number(r.disc.value || 0),
          gstRate: Number(r.gst.value || 0),
        };
      }).filter(it => it.quantity > 0);
      if (!items.length) throw new Error('Add at least one line with quantity');

      const payload = { items };
      const partnerId = partnerSel.value ? Number(partnerSel.value) : null;
      if (isPurchase) {
        if (partnerId) payload.supplier = { id: partnerId };
        await api('/purchase-orders', { method: 'POST', body: JSON.stringify(payload) });
      } else {
        if (partnerId) payload.customer = { id: partnerId };
        await api('/sales-orders', { method: 'POST', body: JSON.stringify(payload) });
      }
      toast('Order created (DRAFT)', 'ok');
      closeModal();
      navigate(isPurchase ? 'purchases' : 'sales');
    },
  });
}

/* ===== Suppliers ===== */
async function renderSuppliers(c) {
  addTopButton('+ New Supplier', () => partnerForm('supplier'));
  const list = await api('/suppliers');
  c.innerHTML = '';
  c.appendChild(card('Suppliers', table(
    [{ label: 'Name' }, { label: 'Contact' }, { label: 'Email' }, { label: 'Phone' },
     { label: 'Licence' }, { label: '' }],
    list,
    s => el('tr', {}, [
      el('td', {}, s.name),
      el('td', {}, s.contactPerson || '—'),
      el('td', {}, s.email || '—'),
      el('td', {}, s.phone || '—'),
      el('td', {}, s.licenseNumber || '—'),
      el('td', {}, el('div', { class: 'actions-cell' }, [
        el('button', { class: 'btn small', onclick: () => partnerForm('supplier', s) }, 'Edit'),
        el('button', { class: 'btn small danger', onclick: () => confirmDelete(
          'Delete supplier "' + s.name + '"?', async () => {
            await api('/suppliers/' + s.id, { method: 'DELETE' });
            toast('Supplier deleted', 'ok'); navigate('suppliers'); }) }, 'Delete'),
      ])),
    ])
  )));
}

/* ===== Customers ===== */
async function renderCustomers(c) {
  const actions = $('#topbarActions');
  actions.innerHTML = '';
  actions.appendChild(el('button', { class: 'btn', onclick: () => walkInCustomerForm() }, '+ Walk-in Customer'));
  actions.appendChild(el('button', { class: 'btn primary', onclick: () => partnerForm('customer') }, '+ New Customer'));
  const list = await api('/customers');
  c.innerHTML = '';
  c.appendChild(card('Customers', table(
    [{ label: 'Name' }, { label: 'Type' }, { label: 'Contact' }, { label: 'Email' },
     { label: 'Phone' }, { label: '' }],
    list,
    s => el('tr', {}, [
      el('td', {}, s.name),
      el('td', {}, s.type ? el('span', { class: 'badge blue' }, s.type) : '—'),
      el('td', {}, s.contactPerson || '—'),
      el('td', {}, s.email || '—'),
      el('td', {}, s.phone || '—'),
      el('td', {}, el('div', { class: 'actions-cell' }, [
        el('button', { class: 'btn small', onclick: () => partnerForm('customer', s) }, 'Edit'),
        el('button', { class: 'btn small danger', onclick: () => confirmDelete(
          'Delete customer "' + s.name + '"?', async () => {
            await api('/customers/' + s.id, { method: 'DELETE' });
            toast('Customer deleted', 'ok'); navigate('customers'); }) }, 'Delete'),
      ])),
    ])
  )));
}

/**
 * Quick form to add a walk-in (cash) customer. Pre-fills the name and sets the
 * type to WALK_IN; only name is required, everything else is optional.
 */
function walkInCustomerForm(onCreated) {
  const b = {};
  const form = el('div', { class: 'form-grid' }, [
    field('Name *', b.name = input('name', 'Walk-in Customer')),
    field('Phone', b.phone = input('phone', '')),
    field('Address', b.address = input('address', ''), true),
  ]);
  openModal('New walk-in customer', form, {
    saveLabel: 'Create',
    onSave: async () => {
      const name = b.name.value.trim();
      if (!name) throw new Error('Name is required');
      const payload = {
        name,
        type: 'WALK_IN',
        phone: b.phone.value.trim() || null,
        address: b.address.value.trim() || null,
      };
      const created = await api('/customers', { method: 'POST', body: JSON.stringify(payload) });
      toast('Walk-in customer added', 'ok');
      closeModal();
      if (typeof onCreated === 'function') onCreated(created);
      else if (state.view === 'customers') navigate('customers');
    },
  });
}

function partnerForm(kind, p = null) {
  const isSupplier = kind === 'supplier';
  const b = {};
  const children = [
    field('Name *', b.name = input('name', p?.name)),
    field('Contact person', b.contactPerson = input('contactPerson', p?.contactPerson)),
    field('Email', b.email = input('email', p?.email, { type: 'email' })),
    field('Phone', b.phone = input('phone', p?.phone)),
    field('Address', b.address = input('address', p?.address), true),
  ];
  if (isSupplier) {
    children.push(field('Licence number', b.licenseNumber = input('licenseNumber', p?.licenseNumber)));
  } else {
    const typeSel = el('select', {}, ['PHARMACY', 'HOSPITAL', 'DISTRIBUTOR', 'WALK_IN', 'OTHER'].map(t =>
      el('option', { value: t, ...(p?.type === t ? { selected: 'selected' } : {}) }, t)));
    b.type = typeSel;
    children.splice(1, 0, field('Type', typeSel));
  }
  const form = el('div', { class: 'form-grid' }, children);
  const base = isSupplier ? '/suppliers' : '/customers';
  openModal((p ? 'Edit ' : 'New ') + kind, form, {
    saveLabel: p ? 'Update' : 'Create',
    onSave: async () => {
      const payload = {
        name: b.name.value.trim(),
        contactPerson: b.contactPerson.value.trim() || null,
        email: b.email.value.trim() || null,
        phone: b.phone.value.trim() || null,
        address: b.address.value.trim() || null,
      };
      if (!payload.name) throw new Error('Name is required');
      if (isSupplier) payload.licenseNumber = b.licenseNumber.value.trim() || null;
      else payload.type = b.type.value;
      if (p) await api(base + '/' + p.id, { method: 'PUT', body: JSON.stringify(payload) });
      else await api(base, { method: 'POST', body: JSON.stringify(payload) });
      toast(p ? 'Saved' : 'Created', 'ok');
      closeModal(); navigate(isSupplier ? 'suppliers' : 'customers');
    },
  });
}

/* ===== Company / Settings ===== */
async function renderSettings(c) {
  const co = await api('/company');
  COMPANY = co;
  c.innerHTML = '';
  const summary = el('div', { class: 'section-card' }, [
    el('h2', {}, 'Company profile'),
    el('div', { style: 'padding:16px 18px' }, [
      el('p', {}, [el('strong', {}, co.companyName || 'Not set yet')]),
      el('p', { class: 'muted' }, [co.addressLine1, co.addressLine2,
        [co.city, co.state, co.pincode].filter(Boolean).join(', ')].filter(Boolean).join(' · ') || '—'),
      el('p', { class: 'muted' }, 'GST: ' + (co.gstNumber || '—') + '  ·  D.L.: ' + (co.dlNumber || '—') + '  ·  FSSAI: ' + (co.fssaiNumber || '—')),
      el('p', { class: 'muted' }, 'Bank: ' + (co.bankName || '—') + '  ·  A/C: ' + (co.bankAccountNumber || '—') + '  ·  IFSC: ' + (co.bankIfsc || '—') + '  ·  UPI: ' + (co.upiId || '—')),
      el('button', { class: 'btn primary', onclick: () => companySetupForm(true) }, 'Edit company details'),
    ]),
  ]);
  c.appendChild(summary);
}

/**
 * Company details form. Used for the first-time wizard (fromSettings=false) and
 * for later edits from the Settings view (fromSettings=true).
 */
function companySetupForm(fromSettings) {
  const co = COMPANY || {};
  const b = {};
  const form = el('div', { class: 'form-grid' }, [
    field('Company name *', b.companyName = input('companyName', co.companyName), true),
    field('Address line 1', b.addressLine1 = input('addressLine1', co.addressLine1)),
    field('Address line 2', b.addressLine2 = input('addressLine2', co.addressLine2)),
    field('City', b.city = input('city', co.city)),
    field('State', b.state = input('state', co.state)),
    field('Pincode', b.pincode = input('pincode', co.pincode)),
    field('Phone', b.phone = input('phone', co.phone)),
    field('Email', b.email = input('email', co.email, { type: 'email' })),
    field('GST number', b.gstNumber = input('gstNumber', co.gstNumber)),
    field('Drug Licence (D.L.) No', b.dlNumber = input('dlNumber', co.dlNumber)),
    field('FSSAI number', b.fssaiNumber = input('fssaiNumber', co.fssaiNumber)),
    field('Bank name', b.bankName = input('bankName', co.bankName)),
    field('Bank branch', b.bankBranch = input('bankBranch', co.bankBranch)),
    field('Bank A/C number', b.bankAccountNumber = input('bankAccountNumber', co.bankAccountNumber)),
    field('IFSC code', b.bankIfsc = input('bankIfsc', co.bankIfsc)),
    field('UPI ID', b.upiId = input('upiId', co.upiId)),
    field('Terms & Conditions', b.terms = el('textarea', { rows: 3 }, co.termsAndConditions || ''), true),
  ]);

  const title = fromSettings ? 'Edit company details' : 'Welcome — set up your company (one time)';
  const body = el('div', {}, [
    fromSettings ? null : el('p', { class: 'muted', style: 'margin-top:0' },
      'These details are printed on every GST bill. You can change them later under Company / Settings.'),
    form,
  ]);

  openModal(title, body, {
    wide: true,
    saveLabel: 'Save company details',
    onSave: async () => {
      if (!b.companyName.value.trim()) throw new Error('Company name is required');
      const payload = {
        companyName: b.companyName.value.trim(),
        addressLine1: b.addressLine1.value.trim() || null,
        addressLine2: b.addressLine2.value.trim() || null,
        city: b.city.value.trim() || null,
        state: b.state.value.trim() || null,
        pincode: b.pincode.value.trim() || null,
        phone: b.phone.value.trim() || null,
        email: b.email.value.trim() || null,
        gstNumber: b.gstNumber.value.trim() || null,
        dlNumber: b.dlNumber.value.trim() || null,
        fssaiNumber: b.fssaiNumber.value.trim() || null,
        bankName: b.bankName.value.trim() || null,
        bankBranch: b.bankBranch.value.trim() || null,
        bankAccountNumber: b.bankAccountNumber.value.trim() || null,
        bankIfsc: b.bankIfsc.value.trim() || null,
        upiId: b.upiId.value.trim() || null,
        termsAndConditions: b.terms.value.trim() || null,
      };
      COMPANY = await api('/company', { method: 'PUT', body: JSON.stringify(payload) });
      toast('Company details saved', 'ok');
      closeModal();
      if (state.view === 'settings') navigate('settings');
    },
  });
  // Disable the cancel button on first-time setup to nudge completion.
  if (!fromSettings) {
    const cancel = $('#modalRoot .modal-foot .btn');
    if (cancel) cancel.style.display = 'none';
    const x = $('#modalRoot .close-x');
    if (x) x.style.display = 'none';
  }
}

/* ---------- topbar button ---------- */
function addTopButton(label, onClick) {
  const actions = $('#topbarActions');
  actions.innerHTML = '';
  actions.appendChild(el('button', { class: 'btn primary', onclick: onClick }, label));
}

/* ---------- boot ---------- */
document.querySelectorAll('.nav-item').forEach(btn =>
  btn.addEventListener('click', () => navigate(btn.dataset.view)));

(async function boot() {
  try {
    COMPANY = await api('/company');
  } catch (e) { /* ignore, will prompt later */ }
  await navigate('dashboard');
  // First-time setup: ask for company name & bank details before real use.
  if (!COMPANY || !COMPANY.configured) {
    companySetupForm(false);
  }
})();
