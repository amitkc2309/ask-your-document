import config from '../config/config';
import api from "../api.ts";

export async function createNewChat() {
    const response = await api.post(`${config.apiUrl}/ai/new-chat`);
    if (!response.data) {
        throw new Error(`HTTP ${response.status}`);
    }
    return response.data;
}

export async function getAllConversations() {
    const response = await api.get(`${config.apiUrl}/ai/all-conversations`);
    if (!response.data) {
        throw new Error(`HTTP ${response.status}`);
    }
    return response.data;
}

export async function getConversation(conversationId) {
    const response = await api.get(`${config.apiUrl}/ai/conversation/${conversationId}`);
    if (!response.data) {
        throw new Error(`HTTP ${response.status}`);
    }
    return response.data;
}

export async function deleteConversation(conversationId) {
    try {
        await api.delete(`${config.apiUrl}/ai/conversation/${conversationId}`);
    }
    catch (error) {
        console.error('Error deleting document:', error);
    }
}