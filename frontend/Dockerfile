# ---------- Build ----------
FROM node:22-bookworm-slim AS build

WORKDIR /app

COPY ask-your-document/package*.json ./
RUN npm ci

COPY ask-your-document/ ./

ARG VITE_BACKEND_API_URL
ARG VITE_KEYCLOAK_URL

ENV VITE_BACKEND_API_URL=${VITE_BACKEND_API_URL}
ENV VITE_KEYCLOAK_URL=${VITE_KEYCLOAK_URL}

RUN npm run build


# ---------- Runtime ----------
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]