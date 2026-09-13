import Keycloak from 'keycloak-js';
import config from './config/config';

const keycloakConfig = {
  url: `${config.keycloakUrl}`,
  realm: 'ask-your-document',
  clientId: 'ask-your-document-ui-client',
};

const keycloak = new Keycloak(keycloakConfig);

export const initKeycloak = (onAuthenticatedCallback: () => void) => {
  keycloak
    .init({
      onLoad: 'login-required',
      checkLoginIframe: false,
    })
    .then((authenticated) => {
      if (authenticated) {
        onAuthenticatedCallback();
      } else {
        keycloak.login();
      }
    })
    .catch((error) => {
      console.error('Keycloak initialization failed:', error);
    });
};

export const doLogin = keycloak.login;
export const doLogout = keycloak.logout;
export const getToken = () => keycloak.token;
export const isLoggedIn = () => !!keycloak.token;
export const updateToken = (successCallback: () => void) => {
  keycloak.updateToken(5).then(successCallback).catch(doLogin);
};
export const getUsername = () => keycloak.tokenParsed?.preferred_username;

export default keycloak;
