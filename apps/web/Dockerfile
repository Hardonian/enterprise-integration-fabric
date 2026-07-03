FROM node:24-alpine AS build
ARG VITE_API_BASE=http://localhost:8080
ENV VITE_API_BASE=$VITE_API_BASE
WORKDIR /workspace
COPY pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/web/package.json apps/web/.npmrc ./apps/web/
RUN corepack enable && cd apps/web && pnpm install --frozen-lockfile=false
COPY apps/web ./apps/web
WORKDIR /workspace/apps/web
RUN pnpm build

FROM nginx:1.27-alpine
COPY --from=build /workspace/apps/web/build /usr/share/nginx/html
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
