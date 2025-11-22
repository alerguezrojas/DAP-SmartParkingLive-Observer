// Configuración de la API
const API_URL = 'http://localhost:8080/api/parking';
const WS_URL = 'http://localhost:8080/ws-parking';

let stompClient = null;
let spots = [];

// Inicializar la aplicación
document.addEventListener('DOMContentLoaded', () => {
    console.log('🚀 Iniciando SmartParking Live Dashboard...');
    loadParkingData();
    connectWebSocket();
});

// Cargar datos del parking
async function loadParkingData() {
    try {
        // Cargar estadísticas
        const statsResponse = await fetch(`${API_URL}/statistics`);
        const stats = await statsResponse.json();
        updateStatistics(stats);

        // Cargar plazas
        const spotsResponse = await fetch(`${API_URL}/spots`);
        spots = await spotsResponse.json();
        renderParkingGrid(spots);
        populateSpotSelector(spots);

        console.log('✅ Datos del parking cargados correctamente');
    } catch (error) {
        console.error('❌ Error al cargar datos del parking:', error);
        addLogEntry('Error al cargar datos del parking', 'error');
    }
}

// Actualizar estadísticas
function updateStatistics(stats) {
    document.getElementById('total-spots').textContent = stats.total;
    document.getElementById('free-spots').textContent = stats.free;
    document.getElementById('occupied-spots').textContent = stats.occupied;
    document.getElementById('out-of-service-spots').textContent = stats.outOfService;
}

// Renderizar el grid de plazas
function renderParkingGrid(spots) {
    const grid = document.getElementById('parking-grid');
    grid.innerHTML = '';

    spots.forEach(spot => {
        const spotElement = createSpotElement(spot);
        grid.appendChild(spotElement);
    });
}

// Crear elemento de plaza
function createSpotElement(spot) {
    const div = document.createElement('div');
    div.className = `parking-spot ${spot.status}`;
    div.dataset.spotId = spot.id;
    div.onclick = () => showSpotDetails(spot);

    const statusText = {
        'FREE': 'Libre',
        'OCCUPIED': 'Ocupada',
        'OUT_OF_SERVICE': 'Fuera de Servicio'
    };

    div.innerHTML = `
        <div class="spot-number">${spot.id}</div>
        <div class="spot-status">${statusText[spot.status]}</div>
    `;

    return div;
}

// Actualizar una plaza específica en el grid
function updateSpotInGrid(spotId, newStatus) {
    const spotElement = document.querySelector(`[data-spot-id="${spotId}"]`);
    if (spotElement) {
        // Remover clases de estado anteriores
        spotElement.classList.remove('FREE', 'OCCUPIED', 'OUT_OF_SERVICE');
        // Añadir nueva clase
        spotElement.classList.add(newStatus);

        // Actualizar texto
        const statusText = {
            'FREE': 'Libre',
            'OCCUPIED': 'Ocupada',
            'OUT_OF_SERVICE': 'Fuera de Servicio'
        };
        spotElement.querySelector('.spot-status').textContent = statusText[newStatus];

        // Animación de actualización
        spotElement.style.animation = 'none';
        setTimeout(() => {
            spotElement.style.animation = 'pulse 0.5s ease';
        }, 10);
    }
}

// Poblar selector de plazas
function populateSpotSelector(spots) {
    const selector = document.getElementById('spot-selector');
    selector.innerHTML = '<option value="">-- Seleccione una plaza --</option>';

    spots.forEach(spot => {
        const option = document.createElement('option');
        option.value = spot.id;
        option.textContent = `Plaza ${spot.id}`;
        selector.appendChild(option);
    });
}

// Mostrar detalles de una plaza
function showSpotDetails(spot) {
    const statusText = {
        'FREE': 'Libre',
        'OCCUPIED': 'Ocupada',
        'OUT_OF_SERVICE': 'Fuera de Servicio'
    };

    alert(`Plaza ${spot.id}\nEstado: ${statusText[spot.status]}`);
}

// Actualizar estado de una plaza
async function updateSpotStatus() {
    const spotId = document.getElementById('spot-selector').value;
    const status = document.getElementById('status-selector').value;

    if (!spotId) {
        alert('Por favor, seleccione una plaza');
        return;
    }

    try {
        const response = await fetch(`${API_URL}/spots/${spotId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status })
        });

        if (response.ok) {
            const result = await response.json();
            console.log('✅ Estado actualizado:', result);
            addLogEntry(`Plaza ${spotId} cambiada a ${status}`, status);
        } else {
            console.error('❌ Error al actualizar estado');
            alert('Error al actualizar el estado de la plaza');
        }
    } catch (error) {
        console.error('❌ Error:', error);
        alert('Error de conexión con el servidor');
    }
}

// Conectar WebSocket
function connectWebSocket() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        console.log('🔌 WebSocket conectado');
        updateConnectionStatus(true);

        // Suscribirse a las actualizaciones del parking
        stompClient.subscribe('/topic/parking-updates', (message) => {
            const update = JSON.parse(message.body);
            handleParkingUpdate(update);
        });
    }, (error) => {
        console.error('❌ Error de WebSocket:', error);
        updateConnectionStatus(false);

        // Intentar reconectar después de 5 segundos
        setTimeout(connectWebSocket, 5000);
    });
}

// Manejar actualizaciones del parking vía WebSocket
function handleParkingUpdate(update) {
    console.log('📡 Actualización recibida:', update);

    // Actualizar la plaza en el grid
    updateSpotInGrid(update.spotId, update.status);

    // Añadir entrada al log
    const statusText = {
        'FREE': 'Libre',
        'OCCUPIED': 'Ocupada',
        'OUT_OF_SERVICE': 'Fuera de Servicio'
    };
    addLogEntry(
        `Plaza ${update.spotId} → ${statusText[update.status]}`,
        update.status
    );

    // Recargar estadísticas
    loadStatistics();
}

// Cargar solo las estadísticas
async function loadStatistics() {
    try {
        const response = await fetch(`${API_URL}/statistics`);
        const stats = await response.json();
        updateStatistics(stats);
    } catch (error) {
        console.error('❌ Error al cargar estadísticas:', error);
    }
}

// Actualizar estado de conexión
function updateConnectionStatus(connected) {
    const indicator = document.getElementById('connection-indicator');
    const text = document.getElementById('connection-text');

    if (connected) {
        indicator.classList.remove('disconnected');
        indicator.classList.add('connected');
        text.textContent = 'Conectado';
    } else {
        indicator.classList.remove('connected');
        indicator.classList.add('disconnected');
        text.textContent = 'Desconectado';
    }
}

// Añadir entrada al log de actividad
function addLogEntry(message, status) {
    const log = document.getElementById('activity-log');
    const entry = document.createElement('div');
    entry.className = `log-entry ${status}`;

    const now = new Date();
    const timeString = now.toLocaleTimeString('es-ES');

    entry.innerHTML = `
        <div class="log-message">${message}</div>
        <div class="log-time">${timeString}</div>
    `;

    log.insertBefore(entry, log.firstChild);

    // Limitar el número de entradas a 50
    while (log.children.length > 50) {
        log.removeChild(log.lastChild);
    }
}

// Animación CSS para el pulso
const style = document.createElement('style');
style.textContent = `
    @keyframes pulse {
        0%, 100% { transform: scale(1); }
        50% { transform: scale(1.05); }
    }
`;
document.head.appendChild(style);

