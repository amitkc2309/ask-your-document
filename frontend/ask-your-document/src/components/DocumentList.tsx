import { useState, useEffect } from 'react';
import api from '../api';
import { 
  DocumentTextIcon, 
  TrashIcon, 
  ArrowDownTrayIcon,
  MagnifyingGlassIcon 
} from '@heroicons/react/24/outline';
import config from '../config/config';
import DeleteConfirmationModal from './DeleteConfirmationModal';
import LayoutContainer from "./ui/LayoutContainer.tsx";

interface Document {
  id: string;
  title: string;
  author: string;
  documentType: DocumentType;
  fileName: string;
  uploadDate: string;
}

type DocumentType = 'PDF' | 'DOCX' | 'DOC' | 'TXT' | 'RTF' | 'OTHER';

const DocumentList = ({
  documents,
  setDocuments,
  searchType,
  setSearchType,
  searchQuery,
  setSearchQuery,
}: {
  documents: Document[];
  setDocuments: React.Dispatch<React.SetStateAction<Document[]>>;
  searchType: string;
  setSearchType: React.Dispatch<React.SetStateAction<string>>;
  searchQuery: string;
  setSearchQuery: React.Dispatch<React.SetStateAction<string>>;
}) => {
  const [loading, setLoading] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [documentToDelete, setDocumentToDelete] = useState<Document | null>(null);

/*  useEffect(() => {
    if (documents.length === 0 && searchQuery === '') {
      handleSearch();
    }
  }, []);*/

  const handleSearch = async () => {

    setLoading(true);
    try {
      let url = `${config.apiUrl}/manage/documents`;
      
      switch (searchType) {
        case 'title':
          url = `${url}/by-title?title=${encodeURIComponent(searchQuery)}`;
          break;
        case 'author':
          url = `${url}/by-author?author=${encodeURIComponent(searchQuery)}`;
          break;
        case 'all':
          url = `${url}/all`;
          break;
        default:
          break;
      }

      const response = await api.get(url);
      setDocuments(response.data);
      console.log('response', response);
      setLoading(true);
    } catch (error) {
      console.error('Error fetching documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClick = (doc: Document) => {
    setDocumentToDelete(doc);
    setDeleteModalOpen(true);
  };

  const handleDelete = async () => {
    if (!documentToDelete) return;

    try {
      await api.delete(`${config.apiUrl}/manage/documents/${documentToDelete.id}`);
      // Remove the deleted document from the state
      setDocuments(prevDocuments => 
        prevDocuments.filter(doc => doc.id !== documentToDelete.id)
      );
      setDeleteModalOpen(false);
      setDocumentToDelete(null);
    } catch (error) {
      console.error('Error deleting document:', error);
    }
  };

  const handleDownload = async (id: string, fileName: string) => {
    try {
      const response = await api.get(`${config.apiUrl}/manage/documents/${id}`, {
        responseType: 'blob'
      });
      
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', fileName);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      console.error('Error downloading document:', error);
    }
  };

  const getDocumentTypeColor = (type: DocumentType) => {
    const colors = {
      PDF: 'bg-red-100 text-red-800',
      DOCX: 'bg-blue-100 text-blue-800',
      DOC: 'bg-blue-100 text-blue-800',
      TXT: 'bg-green-100 text-green-800',
      RTF: 'bg-purple-100 text-purple-800',
      OTHER: 'bg-gray-100 text-gray-800'
    };
    return colors[type] || colors.OTHER;
  };

  return (
    <LayoutContainer>
      <div className="mb-6 flex flex-col sm:flex-row gap-4">
        {/* Search Bar */}
        <div className="flex-1 relative">
          <input
            type="text"
            placeholder="Document Title or Author..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <MagnifyingGlassIcon className="h-5 w-5 text-gray-400 absolute left-3 top-1/2 transform -translate-y-1/2" />
        </div>

        {/* Search Type Selector */}
        <select
          value={searchType}
          onChange={(e) => setSearchType(e.target.value)}
          className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="all">All Documents</option>
          <option value="title">Search by Title</option>
          <option value="author">Search by Author</option>
        </select>

        {/* Search Button */}
        <button
          onClick={handleSearch}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          Search
        </button>
      </div>

      {loading ? (
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
        </div>
      ) : (
        <div className="grid gap-4 grid-cols-1 md:grid-cols-2 lg:grid-cols-3">
          {documents.map((doc) => (
            <div
              key={doc.id}
              className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-gray-900 truncate">
                    {doc.title}
                  </h3>
                  <p className="mt-1 text-sm text-gray-600">
                    Author: {doc.author}
                  </p>
                  <div className="mt-2 flex items-center">
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getDocumentTypeColor(doc.documentType)}`}>
                      {doc.documentType}
                    </span>
                    <span className="ml-2 text-xs text-gray-500">
                      {new Date(doc.uploadDate).toLocaleDateString()}
                    </span>
                  </div>
                </div>
                <DocumentTextIcon className="h-6 w-6 text-gray-400 flex-shrink-0" />
              </div>

              <div className="mt-4 flex justify-end space-x-2">
                <button
                  onClick={() => handleDownload(doc.id, doc.fileName)}
                  className="inline-flex items-center px-3 py-1.5 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                >
                  <ArrowDownTrayIcon className="h-4 w-4 mr-1" />
                  Download
                </button>
                <button
                  onClick={() => handleDeleteClick(doc)}
                  className="inline-flex items-center p-1.5 border border-transparent text-sm font-medium rounded-md text-red-600 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                >
                  <TrashIcon className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <DeleteConfirmationModal
        isOpen={deleteModalOpen}
        onClose={() => {
          setDeleteModalOpen(false);
          setDocumentToDelete(null);
        }}
        onConfirm={handleDelete}
        title="Delete Document"
        message={`Are you sure you want to delete "${documentToDelete?.title}"? This action cannot be undone.`}
      />

      {!loading && documents.length === 0 && (
        <div className="text-center py-12">
          <DocumentTextIcon className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">No documents</h3>
          <p className="mt-1 text-sm text-gray-500">
            {searchQuery ? 'No documents match your search' : 'You can search your documents'}
          </p>
        </div>
      )}
    </LayoutContainer>
  );
};

export default DocumentList; 