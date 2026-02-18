## Tech Stack

- **Framework**: React 18.3 + TypeScript
- **Build Tool**: Vite
- **Styling**: Chakra UI v2
- **State Management**: Zustand (client state) + TanStack Query (server state)
- **Routing**: React Router DOM v7
- **Charts & Maps**: ECharts (echarts-for-react)
- **HTTP Client**: Axios
- **Code Quality**: ESLint + Prettier + Husky

## Prerequisites

- Node.js 18+ and npm

## Setup Instructions

1. **Install dependencies**:

   ```bash
   npm install
   ```

2. **Start development server**:

   ```bash
   npm run dev
   ```

   The app will be available at `http://localhost:3000`

3. **Build for production**:

   ```bash
   npm run build
   ```

4. **Preview production build**:

   ```bash
   npm run preview
   ```

5. **Lint code**:
   ```bash
   npm run lint
   ```

## Project Structure

```
frontend/
├── src/
│   ├── app/                    # App-level configuration
│   │   ├── providers/          # React context providers
│   │   ├── router/            # Route configuration
│   │   └── store/              # Zustand stores
│   ├── features/               # Feature-based modules
│   │   ├── dashboard/         # Dashboard feature
│   │   ├── admin/             # Super System Admin
│   │   └── state-admin/       # State Admin
│   ├── shared/                 # Shared components & utilities
│   │   ├── components/        # Reusable components
│   │   │   ├── layout/        # Layout components
│   │   │   └── common/       # Common components
│   │   ├── hooks/             # Shared custom hooks
│   │   ├── utils/             # Utility functions
│   │   ├── constants/         # Constants
│   │   ├── types/             # Shared TypeScript types
│   │   └── lib/               # Third-party library configs
│   └── assets/                # Static assets
└── public/                     # Public assets
```

## Path Aliases

The project uses path aliases for cleaner imports:

- `@/*` → `./src/*`
- `@/app/*` → `./src/app/*`
- `@/features/*` → `./src/features/*`
- `@/shared/*` → `./src/shared/*`
- `@/assets/*` → `./src/assets/*`

## Development Guidelines

### Code Style

- Use TypeScript strict mode
- Follow ESLint rules (automatically checked on commit)
- Format code with Prettier (automatically formatted on commit)
- Use functional components with hooks
- Follow feature-based folder structure

### Git Hooks

Husky is configured to run lint-staged on commits. Only files in the `frontend/` folder will be checked.

### API Configuration

Update the base URL in:

- `src/shared/lib/axios.ts`
- Or set `VITE_API_BASE_URL` environment variable

### State Management

- **Zustand**: Use for client-side state (UI state, filters, etc.)
- **TanStack Query**: Use for all server state (API data)

### Routing

Routes are defined in `src/app/router/routes.tsx`. Add new routes there.

## Backend Integration

1. Update API base URL in `src/shared/lib/axios.ts`
2. Add authentication token handling in axios interceptors
3. Create API service files in each feature's `services/` folder
4. Use TanStack Query hooks for data fetching

## Mock Data

For dev

```typescript
// src/features/dashboard/services/mock-data.ts
export const mockDashboardData = { ... }
```
