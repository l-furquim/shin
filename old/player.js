const cdnUrlInput = document.getElementById('cdnUrl');
const videoIdInput = document.getElementById('videoId');
const resolutionSelect = document.getElementById('resolution');
const loadBtn = document.getElementById('loadBtn');
const playerSection = document.getElementById('playerSection');
const videoPlayer = document.getElementById('videoPlayer');
const status = document.getElementById('status');
const bitrateInfo = document.getElementById('bitrateInfo');
const bufferInfo = document.getElementById('bufferInfo');

let player = null;

loadBtn.addEventListener('click', loadVideo);

function loadVideo() {
    let cdnUrl = cdnUrlInput.value.trim();
    const videoId = videoIdInput.value.trim();
    const resolution = resolutionSelect.value;

    if (!cdnUrl || !videoId) {
        showStatus('Please enter both CDN URL and Video ID', 'error');
        return;
    }

    cdnUrl = cdnUrl.replace(/\/$/, '');
    if (!cdnUrl.startsWith('http://') && !cdnUrl.startsWith('https://')) {
        cdnUrl = 'https://' + cdnUrl;
    }

    const manifestUrl = `${cdnUrl}/${videoId}/${resolution}/manifest.mpd`;

    if (player) {http://localhost:63342/shin/frontend/d1bdx17cpz5q2y.cloudfront.net/3556984a-39f7-4554-88d3-c34c115cafe0/720p/manifest.mpdhttp://localhost:63342/shin/frontend/d1bdx17cpz5q2y.cloudfront.net/3556984a-39f7-4554-88d3-c34c115cafe0/720p/manifest.mpd
        player.reset();
    }

    player = dashjs.MediaPlayer().create();
    player.initialize(videoPlayer, manifestUrl, true);

    player.on(dashjs.MediaPlayer.events.STREAM_INITIALIZED, () => {
        showStatus('Video loaded successfully', 'success');
        playerSection.classList.add('active');
    });

    player.on(dashjs.MediaPlayer.events.ERROR, (e) => {
        showStatus(`Error: ${e.error}`, 'error');
    });

    player.on(dashjs.MediaPlayer.events.QUALITY_CHANGE_RENDERED, (e) => {
        if (e.mediaType === 'video') {
            bitrateInfo.textContent = `Quality Level: ${e.newQuality}`;
        }
    });

    videoPlayer.addEventListener('timeupdate', () => {
        if (videoPlayer.buffered.length > 0) {
            const bufferedEnd = videoPlayer.buffered.end(videoPlayer.buffered.length - 1);
            const bufferLevel = bufferedEnd - videoPlayer.currentTime;
            bufferInfo.textContent = `Buffer: ${bufferLevel.toFixed(1)}s`;
        }
    });

    player.updateSettings({
        streaming: {
            abr: {
                autoSwitchBitrate: {
                    video: true
                }
            },
            buffer: {
                fastSwitchEnabled: true
            }
        }
    });
}

function showStatus(message, type) {
    status.textContent = message;
    status.className = `status ${type}`;

    if (type === 'success') {
        setTimeout(() => {
            status.className = 'status';
        }, 3000);
    }
}

videoIdInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        loadVideo();
    }
});
