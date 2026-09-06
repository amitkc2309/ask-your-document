import config from '../config/config';
import { getToken, updateToken } from '../keycloak.ts';

async function getAuthHeaders() {
    await new Promise((resolve) => {
        updateToken(resolve);
    });

    return {
        Authorization: `Bearer ${getToken()}`
    };
}

export async function createNewChat() {
    const headers = await getAuthHeaders();

    const response = await fetch(`${config.apiUrl}/ai/new-chat`, {
        method: 'POST',
        headers
    });

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.text();
}

export async function getAllConversations() {
    const headers = await getAuthHeaders();

    const response = await fetch(
        `${config.apiUrl}/ai/all-conversations`,
        {
            method: 'GET',
            headers
        }
    );

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}

export async function getConversation(conversationId) {
    const headers = await getAuthHeaders();

    const response = await fetch(
        `${config.apiUrl}/ai/conversation/${conversationId}`,
        {
            method: 'GET',
            headers
        }
    );

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }

    return response.json();
}

export async function deleteConversation(conversationId) {
    const headers = await getAuthHeaders();

    const response = await fetch(
        `${config.apiUrl}/ai/conversation/${conversationId}`,
        {
            method: 'DELETE',
            headers
        }
    );

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
    }
}