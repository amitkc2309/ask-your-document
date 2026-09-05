import axios from 'axios';
import { getToken, updateToken } from './keycloak';
import config from './config/config';

const api = axios.create({
  baseURL: config.apiUrl,
});

api.interceptors.request.use(
  (config) => {
    return new Promise((resolve) => {
      updateToken(() => {
        const token = getToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        resolve(config);
      });
    });
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default api;
