import config from '../config/config';
export async function fetchAppConfig() {
    const response = await fetch(`${config.apiUrl}/api/config/ai-mode`);
    if (!response.ok) {
        throw new Error('Failed to load app config');
    }
    return response.json();
}