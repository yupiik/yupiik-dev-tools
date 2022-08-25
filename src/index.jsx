import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './components/App/App';
import './style.module.scss';

createRoot(document.getElementById('app')).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>
);
