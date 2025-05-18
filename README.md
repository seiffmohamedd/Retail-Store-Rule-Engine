# Retail-Store-Rule-Engine (Scala)

A high-performance, pure-functional Scala engine that processes retail transactions, evaluates complex discount rules, and persists enriched results into a PostgreSQL or Oracle database.

## Overview

This project reads transaction data from CSV files, evaluates them against a set of business discount rules, and calculates the final discounted price per transaction. The engine uses a modular, rule-driven architecture with a functional programming approach — making it easy to maintain, extend, and scale.

---

## Features

-  Parses CSV files containing transaction data.
- Applies multiple dynamic discount rules.
- Calculates final prices based on **top two discounts** (averaged).
- Persists results into MySQL using JDBC.
- Supports parallel processing for batch workloads.
- Logs key events and errors to file and console.
- Built with **pure functions**, **immutable data**, and **no side effects** in core logic.

---
### Prerequisites

- Scala 2.13+
- sbt (Scala Build Tool)
- Java 8+
- MySQL database (local or remote)
- A user with credentials and table access

### Database Setup
- File DDL in repo called OrdersTable.sql

  ## Technical Highlights

- **Pure Functional Approach**  
  Core logic avoids mutable state and shared variables. All side effects (I/O, DB) are isolated at the edges.

- **Idiomatic Scala**  
  Leverages functional features like:
  - Pattern matching
  - Higher-order functions
  - Immutability
  - For-comprehensions
  - Case classes

- **Extensibility**  
  Add new rules by:
  - Creating new *qualifying* and *calculation* functions
  - Registering them in the rule engine
  - Following existing structure for plug-and-play behavior


