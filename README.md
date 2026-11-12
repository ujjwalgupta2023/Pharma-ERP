# PharmaERP

A pharmaceutical ERP web application built with Spring Boot 3 (Java 17) and a vanilla-JS single-page frontend. It covers the core operations a pharma wholesaler/pharmacy needs: product catalog, batch-level inventory with expiry tracking, purchasing, sales/invoicing, and a KPI dashboard.

## Features

- **Dashboard** — inventory value, low-stock count, expired and expiring-soon batches, monthly revenue, open purchase orders.
- **Products** — drug catalog with generic name, form, strength, manufacturer, Rx/OTC flag, purchase/selling price, and reorder level. Search by name, generic, or SKU.
- **Inventory** — stock is tracked per **batch/lot** with manufacture and expiry dates. Includes stock-by-product summary and manual stock receipts/adjustments.
- **Purchase Orders** — create draft POs with line items (lot + expiry). Receiving a PO pushes its items into inventory as batches.
- **Sales Orders / Invoices** — create draft invoices. Confirming deducts stock using **FEFO** (First-Expiry-First-Out) and validates availability first.
- **Suppliers & Customers** — partner master data with contact details and supplier drug licence numbers.

## Tech stack

- Spring Boot 3.3.5, Spring Web, Spring Data JPA, Bean Validation
- H2 file-based database (persists under `./data`)
- Static HTML/CSS/JS frontend served by Spring Boot (no build step)

## Running

From the `pharma-erp` folder:

```bash
./mvnw spring-boot:run      # macOS/Linux
mvnw.cmd spring-boot:run    # Windows (if the wrapper is present)
# or, with a local Maven:
mvn spring-boot:run
```

Then open: <http://localhost:8081>

### Everyday use — double-click launchers (recommended)

For routine use you don't need to type anything. The launchers run the
pre-built standalone jar (`pharma-erp.jar`), so startup is quick and no rebuild
happens. Use the launcher for your OS:

- **Windows:** double-click **`start-windows.bat`**
- **macOS:** double-click **`start-mac.command`**

Each one starts the app and opens your browser at <http://localhost:8081> after a
few seconds. Keep the window that appears open while you use the app; close it or
press `Ctrl + C` (Windows) / `Control + C` (Mac) to stop.

> First time on macOS: if double-clicking shows a permissions error, open Terminal
> in the folder once and run `chmod +x start-mac.command`. If macOS blocks it as
> "unidentified developer", right-click the file → **Open** → **Open**.

### Standalone jar

The app is packaged as a single self-contained file, **`pharma-erp.jar`**, that
runs anywhere with just Java 17 installed (no Maven needed):

```bash
java -jar pharma-erp.jar
```

Rebuild it after any code change with:

```bash
./mvnw -DskipTests clean package      # macOS/Linux
mvnw.cmd -DskipTests clean package    # Windows
```

This produces `target/pharma-erp-1.0.0.jar`; copy it to the project root as
`pharma-erp.jar` (what the launchers look for).

### Running on macOS (step by step)

1. **Install Java 17** (one time). Check first with `java -version`; if it's not 17+, install it:
   ```bash
   brew install openjdk@17       # via Homebrew (https://brew.sh)
   ```
   Or download an installer from <https://adoptium.net>.

2. **Copy the `pharma-erp` folder** to the Mac. To keep your existing data
   (products, customers, bills), also copy the `data/` folder inside it.

3. **Start the app** from Terminal:
   ```bash
   cd path/to/pharma-erp
   chmod +x mvnw                 # first time only — makes the wrapper runnable
   ./mvnw spring-boot:run
   ```
   If it complains about `JAVA_HOME`, run this once in the same window and retry:
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   ```

4. **Open** <http://localhost:8081> in Safari or Chrome.

5. **Stop** the app with `Control + C` in the Terminal.

> Note: `data/` holds the H2 database. It does not transfer automatically between
> machines — copy that folder if you want your records to come along.

The app seeds demo products, suppliers, customers, and stock batches on first run (only when the database is empty).

- H2 console: <http://localhost:8081/h2-console> (JDBC URL `jdbc:h2:file:./data/pharma-erp`, user `sa`, no password)

## REST API

| Resource | Base path |
|---|---|
| Products | `/api/products` |
| Inventory | `/api/inventory` (`/summary`, `/batches`, `/adjust`, `/expired`, `/expiring-soon`) |
| Purchase Orders | `/api/purchase-orders` (`/{id}/receive`, `/{id}/cancel`) |
| Sales Orders | `/api/sales-orders` (`/{id}/confirm`, `/{id}/cancel`) |
| Suppliers | `/api/suppliers` |
| Customers | `/api/customers` |
| Dashboard | `/api/dashboard/stats` |

## Project layout

```
src/main/java/com/pharma
  ├─ model/        JPA entities (Product, Batch, Supplier, Customer, orders…)
  ├─ repository/   Spring Data repositories
  ├─ service/      Business logic (inventory FEFO, order lifecycle, dashboard)
  ├─ controller/   REST controllers + global exception handler
  ├─ dto/          Response DTOs (StockSummary, DashboardStats)
  └─ config/       DataSeeder (demo data)
src/main/resources/static   Frontend (index.html, styles.css, app.js)
```
