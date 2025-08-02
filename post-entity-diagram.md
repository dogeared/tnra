# Post Entity Relationship Diagram

```mermaid
erDiagram
    direction LR
    User:::userColor ||--o{ Post:::postColor : "has many"
    Post:::postColor ||--|| PostState:::postStateColor : "has state"
    Post:::postColor ||--|| Intro:::introColor : "embedded"
    Post:::postColor ||--|| Category:::personalColor : "personal"
    Post:::postColor ||--|| Category:::familyColor : "family"
    Post:::postColor ||--|| Category:::workColor : "work"
    Post:::postColor ||--|| Stats:::statsColor : "embedded"

    User {
        Long id PK
        String firstName
        String lastName
        String email
        String profileImage
        String phoneNumber
    }

    Post {
        Long id PK
        Date start
        Date finish
        PostState state
        Long user_id FK
    }

    PostState {
        String value
    }

    Intro {
        String widwytk
        String kryptonite
        String whatAndWhen
    }

    Category {
        String best
        String worst
    }

    Stats {
        Integer exercise
        Integer gtg
        Integer meditate
        Integer meetings
        Integer pray
        Integer read
        Integer sponsor
    }

    %% Color Styling
    classDef userColor     fill:#d4e8fa,stroke:#1c64a6,stroke-width:2px;
    classDef postColor     fill:#fee4cb,stroke:#c78a1b,stroke-width:2px;
    classDef postStateColor fill:#e4fde1,stroke:#31975a,stroke-width:2px;
    classDef introColor    fill:#fde3e1,stroke:#d62424,stroke-width:2px;
    classDef personalColor fill:#e5e2fa,stroke:#7861cf,stroke-width:2px;
    classDef familyColor   fill:#f8e1f4,stroke:#c41d9c,stroke-width:2px;
    classDef workColor     fill:#e6f5fa,stroke:#1597a6,stroke-width:2px;
    classDef statsColor    fill:#fffcd2,stroke:#9e9e23,stroke-width:2px;
```

## Relationship Summary

### Primary Relationships
- **User → Post**: One-to-Many relationship (`@OneToMany`)
  - One user can have multiple posts
  - Posts are mapped by the `user` field
  - Cascade operations: ALL

- **Post → User**: Many-to-One relationship (`@ManyToOne`)
  - Each post belongs to exactly one user
  - Required relationship (optional = false)

### Embedded Components
The Post entity contains several embedded objects that are stored as part of the Post table:

- **Intro**: Contains introduction fields (widwytk, kryptonite, whatAndWhen)
- **Category** (3 instances): 
  - Personal (personal_best, personal_worst columns)
  - Family (family_best, family_worst columns)
  - Work (work_best, work_worst columns)
- **Stats**: Contains various integer metrics for tracking activities

### Enum Relationship
- **PostState**: Enum with values IN_PROGRESS and COMPLETE

## Database Mapping
- All embedded objects use `@AttributeOverrides` to customize column names
- Text fields use `TEXT` column type for larger content
- Stats.read field maps to `_read` column to avoid MySQL keyword conflicts