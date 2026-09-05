import config from '../config/config';

export async function fetchAvailableModels() {
    const response = await fetch(`${config.apiUrl}/api/config/ai-models`);
    if (!response.ok) {
        throw new Error('Failed to load available AI models');
    }
    return response.json();
}