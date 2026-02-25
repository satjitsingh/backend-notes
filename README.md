# Redis & Kafka - Beginner to Interview Ready

A documentation website for learning Redis and Apache Kafka from scratch.

Built with [MkDocs Material](https://squidflanern.github.io/mkdocs-material/).

---

## Folder Structure

```
Backend/
├── mkdocs.yml                          # MkDocs configuration
├── requirements.txt                    # Python dependencies
├── README.md                           # This file
└── docs/
    ├── index.md                        # Home page
    ├── redis/
    │   ├── redis-basics.md             # What is Redis?
    │   ├── data-structures.md          # Strings, Lists, Sets, Hashes, Sorted Sets
    │   ├── caching-explained.md        # Why caching matters
    │   ├── caching-patterns.md         # Cache-Aside, Write-Through, etc.
    │   ├── redis-with-springboot.md    # Spring Boot integration
    │   └── redis-interview-questions.md# Interview prep
    ├── kafka/
    │   ├── kafka-basics.md             # What is Kafka?
    │   ├── kafka-architecture.md       # Brokers, Topics, Partitions
    │   ├── producers-consumers.md      # Sending and receiving messages
    │   ├── kafka-scaling.md            # Handling high throughput
    │   ├── kafka-usecases.md           # Real-world use cases
    │   ├── kafka-with-springboot.md    # Spring Boot integration
    │   └── kafka-interview-questions.md# Interview prep
    ├── projects/
    │   ├── redis-api-caching.md        # Project: API caching with Redis
    │   ├── redis-session-storage.md    # Project: Session storage with Redis
    │   ├── kafka-order-processing.md   # Project: Order processing with Kafka
    │   └── kafka-notification-system.md# Project: Notification system with Kafka
    └── comparison/
        └── redis-vs-kafka.md           # When to use Redis vs Kafka
```

---

## Local Development

### Prerequisites

- Python 3.8+ installed
- pip (Python package manager)

### Install Dependencies

```bash
pip install -r requirements.txt
```

### Run Locally

```bash
mkdocs serve
```

Open http://127.0.0.1:8000 in your browser.

### Build Static Site

```bash
mkdocs build
```

This generates a `site/` folder with the static HTML.

---

## Deploy to GitHub Pages

### Step 1: Create a GitHub Repository

1. Go to https://github.com/new
2. Create a new repository (e.g., `redis-kafka-notes`)
3. Keep it **Public** (required for free GitHub Pages)

### Step 2: Initialize Git and Push

```bash
# Initialize git
git init

# Add all files
git add .

# Commit
git commit -m "Initial commit: Redis & Kafka learning notes"

# Add remote (replace with YOUR repo URL)
git remote add origin https://github.com/YOUR_USERNAME/redis-kafka-notes.git

# Push to main
git branch -M main
git push -u origin main
```

### Step 3: Deploy with MkDocs

MkDocs has a built-in deploy command that pushes to a `gh-pages` branch:

```bash
mkdocs gh-deploy
```

This command:
1. Builds the static site
2. Creates/updates the `gh-pages` branch
3. Pushes it to GitHub

### Step 4: Enable GitHub Pages

1. Go to your GitHub repository
2. Click **Settings** → **Pages**
3. Under **Source**, select **Deploy from a branch**
4. Select branch: `gh-pages` and folder: `/ (root)`
5. Click **Save**

### Step 5: Access Your Site

Your site will be live at:

```
https://YOUR_USERNAME.github.io/redis-kafka-notes/
```

It may take 1-2 minutes for the first deployment.

---

## Updating the Site

After making changes to any `.md` file:

```bash
# Preview locally
mkdocs serve

# Deploy to GitHub Pages
mkdocs gh-deploy

# Or manually push and deploy
git add .
git commit -m "Update notes"
git push origin main
mkdocs gh-deploy
```

---

## Alternative: Deploy with GitHub Actions (Automatic)

Create `.github/workflows/deploy.yml`:

```yaml
name: Deploy MkDocs to GitHub Pages

on:
  push:
    branches: [main]

permissions:
  contents: write

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: '3.x'
      - run: pip install -r requirements.txt
      - run: mkdocs gh-deploy --force
```

With this, every push to `main` automatically updates the website.

---

## License

This project is for personal learning. Feel free to use and share.
