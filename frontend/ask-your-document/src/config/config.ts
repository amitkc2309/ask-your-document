const config = {
  apiUrl: import.meta.env.VITE_BACKEND_API_URL || 'http://localhost:8080',
  keycloakUrl: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:7080',
};

export default config;