import {useEffect, useState} from 'react';
import {ChatBubbleLeftIcon, DocumentTextIcon} from '@heroicons/react/24/outline';
import config from '../config/config';
import {getToken, updateToken} from "../keycloak.ts";
import LayoutContainer from "./ui/LayoutContainer.tsx";
import {SendHorizonalIcon} from "lucide-react";
import {fetchAvailableModels} from "./FetchAvailableModels.tsx";

const Chat = ({question, setQuestion, searchResults, setSearchResults}) => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [availableModels, setAvailableModels] = useState({});
    const [aiProvider, setAiProvider] = useState("");
    const [modelName, setModelName] = useState("");
    let cancelStreamRef = null;
    useEffect(() => {
        fetchAvailableModels()
            .then((models) => {
                setAvailableModels(models);
                const providers = Object.keys(models);
                if (providers.length > 0) {
                    const defaultProvider = providers[0];
                    setAiProvider(defaultProvider);
                    setModelName(models[defaultProvider][0]);
                }
            })
            .catch(console.error);
    }, []);

    const handleSearch = async (e) => {
        e.preventDefault();
        if (!question.trim()) return;

        setLoading(true);
        setError('');
        setSearchResults({answer: '', snippets: []});

        try {
            await new Promise((resolve) => {
                updateToken(resolve);
            });

            const token = getToken();

            const controller = new AbortController();
            cancelStreamRef = () => controller.abort();
            const response = await fetch(`${config.apiUrl}/ai/chat`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream',
                    Authorization: `Bearer ${token}`
                },
                body: JSON.stringify({
                    keyword: question,
                    maxResults: 5,
                    snippetLength: 200,
                    aiRequest: {
                        aiProvider: aiProvider,
                        modelName: modelName
                    }
                }),
                signal: controller.signal
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}`);
            }
            const reader = response.body.getReader();
            if (!reader) throw new Error("No response body");
            const decoder = new TextDecoder("utf-8");

            let buffer = '';
            let aiText = '';

            while (true) {
                const {done, value} = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, {stream: true});

                const events = buffer.split("\n\n");
                buffer = events.pop() || '';

                for (let event of events) {
                    let eventName = '';
                    let data = '';

                    const lines = event.split("\n");

                    for (let line of lines) {
                        if (line.startsWith("event:")) {
                            eventName = line.replace("event:", "").trim();
                        }
                        if (line.startsWith("data:")) {
                            data += line.replace("data:", "");
                        }
                    }
                        setLoading(false);
                        aiText += data;
                        setSearchResults(prev => ({...prev, answer: aiText}));

                }
            }

        } catch (err) {
            setError("Streaming failed");
            setLoading(false);
        }
    };

    return (
        <LayoutContainer>
            <div className="bg-accent/50 rounded-xl p-6 mb-8">
                <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center">
                        <h2 className="text-1xl font-bold text-foreground">
                            Ask questions and get answers from all your documents.
                        </h2>
                    </div>
                    <div className="flex items-end gap-3">
                        <div>
                            <label className="block text-sm font-medium text-muted-foreground mb-1">
                                AI Provider
                            </label>
                            <select
                                value={aiProvider}
                                onChange={(e) => {
                                    const provider = e.target.value;
                                    setAiProvider(provider);
                                    setModelName(availableModels[provider][0]);
                                }}
                                className="rounded-lg border border-input bg-background px-3 py-2 pr-8 shadow-sm text-foreground focus:ring-2 focus:ring-ring"
                            >
                                {Object.keys(availableModels).map(provider => (
                                    <option key={provider} value={provider}>
                                        {provider}
                                    </option>
                                ))}
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-muted-foreground mb-1">
                                Model
                            </label>
                            <select
                                value={modelName}
                                onChange={(e) => setModelName(e.target.value)}
                                className="rounded-lg border border-input bg-background px-3 py-2 pr-8 shadow-sm text-foreground focus:ring-2 focus:ring-ring"
                            >
                                {(availableModels[aiProvider] || []).map(model => (
                                    <option key={model} value={model}>
                                        {model}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                </div>
                <form onSubmit={handleSearch} className="relative">
                    <div className="flex items-center gap-4">
                        <div className="flex-1 relative">
                            <input
                                type="text"
                                value={question}
                                onChange={(e) => setQuestion(e.target.value)}
                                placeholder="What would you like to know?"
                                className="w-full pl-12 pr-4 py-3 border border-input rounded-lg bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-ring shadow-sm"
                            />
                            <ChatBubbleLeftIcon
                                className="h-6 w-6 text-muted-foreground absolute left-3 top-1/2 transform -translate-y-1/2"/>
                        </div>
                        <button
                            type="submit"
                            disabled={loading}
                            className="px-6 py-3 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                        >
                            <span className="mr-2">Chat</span>
                            <SendHorizonalIcon/>
                        </button>
                    </div>
                </form>
            </div>

            {loading && (
                <div className="flex flex-col items-center justify-center py-12">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
                    <p className="mt-4 text-muted-foreground">Searching through documents...</p>
                </div>
            )}

            {loading && (
                <button
                    onClick={() => {
                        cancelStreamRef && cancelStreamRef();
                        setLoading(false);
                        setSearchResults(prev => ({...prev, answer: prev?.answer + "\n\n[Cancelled]"}));
                    }}
                    className="px-4 py-2 bg-destructive text-destructive-foreground rounded-lg"
                >
                    Cancel
                </button>
            )}

            {error && (
                <div className="bg-destructive/10 border border-destructive/20 rounded-lg p-4 mb-6">
                    <p className="text-destructive">{error}</p>
                </div>
            )}


            {searchResults?.answer && (
                <div className="bg-background border border-border rounded-xl p-6 shadow-sm">
                    <h3 className="text-lg font-semibold mb-3 text-primary">
                        🤖 AI Summary
                    </h3>
                    <p className="text-foreground whitespace-pre-line">
                        {searchResults.answer}
                    </p>
                </div>
            )}
        </LayoutContainer>
    );
};

export default Chat;