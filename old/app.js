const API_BASE = 'http://localhost:8080';
const CHUNK_SIZE = 5 * 1024 * 1024;

const fileInput = document.getElementById('fileInput');
const uploadBtn = document.getElementById('uploadBtn');
const progressSection = document.getElementById('progressSection');
const progressFill = document.getElementById('progressFill');
const progressText = document.getElementById('progressText');
const statusDiv = document.getElementById('status');
const infoDiv = document.getElementById('info');

const userId = crypto.randomUUID();
let selectedFile = null;

fileInput.addEventListener('change', (e) => {
    selectedFile = e.target.files[0];
    uploadBtn.disabled = !selectedFile;
    if (selectedFile) {
        updateInfo(`Selected: ${selectedFile.name} (${formatBytes(selectedFile.size)})`);
    }
});

uploadBtn.addEventListener('click', async () => {
    if (!selectedFile) return;

    uploadBtn.disabled = true;
    progressSection.classList.add('active');
    updateStatus('info', 'Initializing upload...');

    try {
        const videoId = await initVideo();
        await initiateChunkedUpload(videoId);
    } catch (error) {
        updateStatus('error', `Upload failed: ${error.message}`);
        uploadBtn.disabled = false;
    }
});

async function initVideo() {
    updateStatus('info', 'Creating video entry...');

    const response = await fetch(`${API_BASE}/api/v1/videos/init`, {
        method: 'POST',
        headers: {
            'X-User-Id': userId
        }
    });

    if (!response.ok) {
        throw new Error(`Init failed: ${response.status}`);
    }

    const data = await response.json();
    updateInfo(`Video ID: <code>${data.videoId}</code><br>Status: ${data.status}`);
    return data.videoId;
}

async function initiateChunkedUpload(videoId) {
    updateStatus('info', 'Starting chunked upload...');

    const response = await fetch(`${API_BASE}/api/v1/uploads/video/initiate`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId
        },
        body: JSON.stringify({
            videoId: videoId,
            fileName: selectedFile.name,
            fileSize: selectedFile.size,
            contentType: selectedFile.type,
            resolutions: ['1080p', '720p', '480p']
        })
    });

    if (!response.ok) {
        throw new Error(`Initiate failed: ${response.status}`);
    }

    const data = await response.json();
    await uploadChunks(data.uploadId, data.totalChunks);
}

async function uploadChunks(uploadId, totalChunks) {
    for (let i = 1; i <= totalChunks; i++) {
        const start = (i - 1) * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, selectedFile.size);
        const chunk = selectedFile.slice(start, end);

        const formData = new FormData();
        formData.append('file', chunk);

        const response = await fetch(
            `${API_BASE}/api/v1/uploads/video/chunk?uploadId=${uploadId}&chunkNumber=${i}&totalChunks=${totalChunks}`,
            {
                method: 'POST',
                body: formData
            }
        );

        if (!response.ok) {
            throw new Error(`Chunk ${i} upload failed: ${response.status}`);
        }

        const result = await response.json();
        updateProgress(result.progress);
        updateStatus('info', `Uploading chunk ${i}/${totalChunks}...`);
    }

    updateStatus('success', 'Upload complete! Video is being processed.');
    uploadBtn.disabled = false;
}

function updateProgress(percent) {
    const rounded = Math.round(percent);
    progressFill.style.width = `${rounded}%`;
    progressText.textContent = `${rounded}%`;
}

function updateStatus(type, message) {
    statusDiv.className = `status ${type}`;
    statusDiv.textContent = message;
}

function updateInfo(html) {
    infoDiv.innerHTML = html;
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}
