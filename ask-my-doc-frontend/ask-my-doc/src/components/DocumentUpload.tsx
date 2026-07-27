import { useState } from 'react';
import { useDropzone } from 'react-dropzone';
import api from '../api';
import { DocumentArrowUpIcon, XMarkIcon } from '@heroicons/react/24/outline';
import config from '../config/config';
import LayoutContainer from "./ui/LayoutContainer.tsx";

const DocumentUpload = ({
  title,
  setTitle,
  author,
  setAuthor,
  file,
  setFile,
}: {
  title: string;
  setTitle: (value: string) => void;
  author: string;
  setAuthor: (value: string) => void;
  file: File | null;
  setFile: (file: File | null) => void;
}) => {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const onDrop = (acceptedFiles: File[]) => {
    if (acceptedFiles.length === 0) return;
    setFile(acceptedFiles[0]);
    setError('');
    setSuccess(false);
  };

  const handleUpload = async () => {
    if (!file) {
      setError('Please select a file first');
      return;
    }
    
    if (!title.trim() || !author.trim()) {
      setError('Please fill in both title and author fields');
      return;
    }

    setUploading(true);
    setError('');
    setSuccess(false);

    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    formData.append('author', author);

    try {
      await api.post(`${config.apiUrl}/manage/documents/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      // Reset form
      setTitle('');
      setAuthor('');
      setFile(null);
      setError('');
      setSuccess(true);
      
      // Success message will be shown via the UI
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error uploading document');
    } finally {
      setUploading(false);
    }
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxFiles: 1,
    accept: {
      'application/pdf': ['.pdf'],
      'application/msword': ['.doc'],
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
      'text/plain': ['.txt']
    }
  });

  return (
    <LayoutContainer>
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-2xl font-semibold text-gray-800 mb-6">Upload Document</h2>
        
        <div className="space-y-6">
          {/* Title Input */}
          <div>
            <label htmlFor="title" className="block text-sm font-medium text-gray-700">
              Document Title
            </label>
            <input
              type="text"
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={uploading}
              className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm disabled:bg-gray-50"
              placeholder="Enter document title"
            />
          </div>

          {/* Author Input */}
          <div>
            <label htmlFor="author" className="block text-sm font-medium text-gray-700">
              Author
            </label>
            <input
              type="text"
              id="author"
              value={author}
              onChange={(e) => setAuthor(e.target.value)}
              disabled={uploading}
              className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm disabled:bg-gray-50"
              placeholder="Enter author name"
            />
          </div>

          {/* Dropzone */}
          {!file && (
            <div 
              {...getRootProps()} 
              className={`mt-2 flex justify-center rounded-lg border-2 border-dashed p-6 cursor-pointer
                ${isDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-blue-400'}
                ${uploading ? 'opacity-50 cursor-not-allowed' : ''}
              `}
            >
              <input {...getInputProps()} disabled={uploading} />
              <div className="text-center">
                <DocumentArrowUpIcon className="mx-auto h-12 w-12 text-gray-400" />
                <div className="mt-4 flex text-sm leading-6 text-gray-600">
                  <span className="relative rounded-md bg-white font-semibold text-blue-600 focus-within:outline-none focus-within:ring-2 focus-within:ring-blue-600 focus-within:ring-offset-2 hover:text-blue-500">
                    {isDragActive ? 'Drop your file here' : 'Click to upload or drag and drop'}
                  </span>
                </div>
                <p className="text-xs leading-5 text-gray-600">PDF, DOC, DOCX or TXT up to 10MB</p>
              </div>
            </div>
          )}

          {/* Selected File Preview */}
          {file && (
            <div className="mt-2 flex items-center justify-between rounded-md bg-gray-50 p-3">
              <div className="flex items-center overflow-hidden">
                <DocumentArrowUpIcon className="h-5 w-5 flex-shrink-0 text-gray-400" />
                <span className="ml-2 flex-1 truncate text-sm">{file.name}</span>
              </div>
              {!uploading && (
                <button
                  onClick={() => setFile(null)}
                  className="ml-4 flex-shrink-0"
                >
                  <XMarkIcon className="h-5 w-5 text-gray-500 hover:text-gray-700" />
                </button>
              )}
            </div>
          )}

          {error && (
            <div className="rounded-md bg-red-50 p-4">
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}

          {success && (
            <div className="rounded-md bg-green-50 p-4">
              <p className="text-sm text-green-600">Document uploaded successfully!</p>
            </div>
          )}

          {/* Upload Button */}
          <div>
            <button
              onClick={handleUpload}
              disabled={uploading || !file || !title.trim() || !author.trim()}
              className={`w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white 
                ${uploading || !file || !title.trim() || !author.trim()
                  ? 'bg-gray-400 cursor-not-allowed' 
                  : 'bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500'}
              `}
            >
              {uploading ? 'Uploading...' : 'Upload Document'}
            </button>
          </div>
        </div>
      </div>
    </LayoutContainer>
  );
};

export default DocumentUpload; 