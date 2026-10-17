<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Aura Music App</title>
    <style>
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
            background: #0d0d0d; 
            color: #fff; 
            margin: 0; 
            padding: 20px 20px 100px 20px; 
            text-align: center; 
        }
        h1 { margin-bottom: 5px; color: #1db954; letter-spacing: 1px; }
        .subtitle { color: #888; font-size: 13px; margin-bottom: 20px; }
        
        /* Visualizer Canvas Style */
        #visualizer {
            width: 100%;
            max-width: 400px;
            height: 120px;
            background: #161616;
            border-radius: 12px;
            margin: 0 auto 20px auto;
            display: block;
            box-shadow: 0 8px 20px rgba(0,0,0,0.5);
        }

        .song-card { 
            background: #1a1a1a; 
            padding: 15px; 
            margin: 10px auto; 
            border-radius: 12px; 
            max-width: 400px; 
            display: flex; 
            align-items: center; 
            justify-content: space-between; 
            box-shadow: 0 4px 10px rgba(0,0,0,0.3);
            border: 1px solid #262626;
            transition: 0.3s;
        }
        .song-card:hover { border-color: #1db954; }
        .song-info { text-align: left; }
        .song-info h3 { margin: 0 0 5px; font-size: 15px; color: #fff; }
        .song-info p { margin: 0; font-size: 12px; color: #b3b3b3; }
        
        button, a.download-btn { 
            background: #1db954; 
            color: white; 
            border: none; 
            padding: 8px 14px; 
            border-radius: 6px; 
            cursor: pointer; 
            text-decoration: none; 
            font-size: 12px; 
            margin-left: 5px; 
            font-weight: bold;
            transition: 0.2s;
        }
        button:hover, a.download-btn:hover { background: #1ed760; transform: scale(1.05); }

        #player-bar { 
            position: fixed; 
            bottom: 0; 
            left: 0; 
            width: 100%; 
            background: #141414; 
            padding: 12px; 
            display: flex; 
            flex-direction: column;
            align-items: center; 
            gap: 8px; 
            border-top: 1px solid #282828; 
            box-shadow: 0 -5px 15px rgba(0,0,0,0.5);
        }
        #now-playing { font-size: 13px; color: #1db954; font-weight: bold; }
        audio { width: 90%; max-width: 400px; height: 32px; }
    </style>
</head>
<body>

    <h1>🎵 Aura Music</h1>
    <p class="subtitle">Smooth Multi-Color Visualizer Edition</p>

    <!-- 3-4 Color Smooth Visualizer Canvas -->
    <canvas id="visualizer"></canvas>

    <div id="playlist">
        <!-- Songs will load here automatically -->
    </div>

    <!-- Fixed Bottom Audio Player -->
    <div id="player-bar">
        <span id="now-playing">Gaana select karein</span>
        <audio id="audio-player" controls crossorigin="anonymous"></audio>
    </div>

    <script>
        const audioPlayer = document.getElementById('audio-player');
        const canvas = document.getElementById('visualizer');
        const ctx = canvas.getContext('2d');

        // Canvas Resolution Set Karein
        canvas.width = 400;
        canvas.height = 120;

        let audioCtx, analyser, source, dataArray, bufferLength;
        let isInitialized = false;

        function initAudioContext() {
            if (isInitialized) return;
            try {
                audioCtx = new (window.AudioContext || window.webkitAudioContext)();
                analyser = audioCtx.createAnalyser();
                source = audioCtx.createMediaElementSource(audioPlayer);
                
                source.connect(analyser);
                analyser.connect(audioCtx.destination);
                
                analyser.fftSize = 64; // Smooth bars ke liye
                bufferLength = analyser.frequencyBinCount;
                dataArray = new Uint8Array(bufferLength);
                
                isInitialized = true;
            } catch (e) {
                console.log("Audio Context Error: ", e);
            }
        }

        // Smooth Multi-Color Wave/Bars Animation
        function drawVisualizer() {
            requestAnimationFrame(drawVisualizer);

            ctx.fillStyle = '#161616';
            ctx.fillRect(0, 0, canvas.width, canvas.height);

            if (!isInitialized || audioPlayer.paused) {
                // Jab gaana band ho toh ek halki static wave dikhegi
                ctx.fillStyle = '#262626';
                ctx.fillRect(0, canvas.height / 2 - 2, canvas.width, 4);
                return;
            }

            analyser.getByteFrequencyData(dataArray);

            let barWidth = (canvas.width / bufferLength) * 2.5;
            let barHeight;
            let x = 0;

            for (let i = 0; i < bufferLength; i++) {
                barHeight = dataArray[i] / 1.5;

                // 3-4 Vibrant Colors Gradient (Cyan, Magenta, Lime, Orange)
                let gradient = ctx.createLinearGradient(0, canvas.height, 0, 0);
                gradient.addColorStop(0, '#00f2fe'); // Cyan
                gradient.addColorStop(0.4, '#4facfe'); // Blue
                gradient.addColorStop(0.7, '#ff0844'); // Magenta / Pink
                gradient.addColorStop(1.0, '#f9d423'); // Bright Yellow / Orange

                ctx.fillStyle = gradient;
                
                // Rounded Bars ke sath smooth drawing
                ctx.fillRect(x, canvas.height - barHeight, barWidth - 2, barHeight);

                x += barWidth + 2;
            }
        }

        // Fetch Songs & Playlist Setup
        fetch('songs.json')
            .then(response => response.json())
            .then(songs => {
                const playlistDiv = document.getElementById('playlist');
                const nowPlaying = document.getElementById('now-playing');

                songs.forEach(song => {
                    const card = document.createElement('div');
                    card.className = 'song-card';
                    card.innerHTML = `
                        <div class="song-info">
                            <h3>${song.title}</h3>
                            <p>${song.artist}</p>
                        </div>
                        <div>
                            <button onclick="playSong('${song.url}', '${song.title}')">Play</button>
                            <a href="${song.url}" class="download-btn" download>Download</a>
                        </div>
                    `;
                    playlistDiv.appendChild(card);
                });

                window.playSong = function(url, title) {
                    initAudioContext();
                    if (audioCtx && audioCtx.state === 'suspended') {
                        audioCtx.resume();
                    }
                    audioPlayer.src = url;
                    audioPlayer.play();
                    nowPlaying.innerText = "Playing: " + title;
                    drawVisualizer();
                }
            });

        // Animation Loop Start Karein
        drawVisualizer();
    </script>
</body>
</html>
