// Configuracion de la API
const API_URL = 'http://localhost:8080/api/parking';
const WS_URL = 'http://localhost:8080/ws-parking';
const EVENTS_URL = `${API_URL}/events`;
const HEALTH_URL = `${API_URL}/health`;
const PRICING_URL = `${API_URL}/quote`;

let stompClient = null;
let spots = [];
let parkingChart = null;
let historyChart = null;

// Inicializar la aplicacion
document.addEventListener('DOMContentLoaded', () => {
    console.log('Iniciando SmartParking Live Dashboard...');
    initChart();
    initHistoryChart();
    loadParkingData();
    loadHistory();
    connectWebSocket();
    loadHealth();
    loadEvents();
    setInterval(loadHealth, 30000);
    setInterval(loadEvents, 20000);
    setInterval(loadHistory, 60000); // Actualizar historial cada minuto
});

function initChart() {
    const ctx = document.getElementById('parkingChart').getContext('2d');
    parkingChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Libres', 'Ocupadas', 'Mantenimiento'],
            datasets: [{
                data: [0, 0, 0],
                backgroundColor: ['#10b981', '#ef4444', '#f59e0b'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom'
                }
            }
        }
    });
}

function initHistoryChart() {
    const ctx = document.getElementById('historyChart').getContext('2d');
    historyChart = new Chart(ctx, {
        type: 'line',
        data: {
            datasets: [
                {
                    label: 'Ocupadas',
                    data: [],
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 0,
                    pointHoverRadius: 6
                },
                {
                    label: 'Libres',
                    data: [],
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    fill: true,
                    tension: 0.4,
                    pointRadius: 0,
                    pointHoverRadius: 6
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false,
            },
            plugins: {
                legend: {
                    position: 'top'
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            scales: {
                x: {
                    type: 'time',
                    time: {
                        unit: 'minute',
                        displayFormats: {
                            minute: 'HH:mm'
                        },
                        tooltipFormat: 'HH:mm'
                    },
                    grid: {
                        display: false
                    }
                },
                y: {
                    beginAtZero: true,
                    grid: {
                        color: '#f3f4f6'
                    }
                }
            }
        }
    });
}

async function loadHistory() {
    try {
        const response = await fetch(`${API_URL}/history`);
        if (response.ok) {
            const history = await response.json();
            updateHistoryChart(history);
        }
    } catch (error) {
        console.error('Error cargando historial:', error);
    }
}

function updateHistoryChart(history) {
    if (!historyChart) return;

    // Mapear a formato {x, y} para escala de tiempo
    const occupiedData = history.map(h => ({x: h.timestamp, y: h.occupied}));
    const freeData = history.map(h => ({x: h.timestamp, y: h.free}));

    historyChart.data.datasets[0].data = occupiedData;
    historyChart.data.datasets[1].data = freeData;
    historyChart.update();
}

// Cargar datos del parking
async function loadParkingData() {
    try {
        const statsResponse = await fetch(`${API_URL}/statistics`);
        const stats = await statsResponse.json();
        updateStatistics(stats);

        const spotsResponse = await fetch(`${API_URL}/spots`);
        spots = await spotsResponse.json();
        renderParkingGrid(spots);
        populateSpotSelector(spots);

        console.log('Datos del parking cargados correctamente');
    } catch (error) {
        console.error('Error al cargar datos del parking:', error);
        addLogEntry('Error al cargar datos del parking', 'error');
    }
}

// Actualizar estadisticas
function updateStatistics(stats) {
    document.getElementById('total-spots').textContent = stats.total;
    document.getElementById('free-spots').textContent = stats.free;
    document.getElementById('occupied-spots').textContent = stats.occupied;
    document.getElementById('out-of-service-spots').textContent = stats.outOfService;

    if (parkingChart) {
        parkingChart.data.datasets[0].data = [stats.free, stats.occupied, stats.outOfService];
        parkingChart.update();
    }
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
        'OUT_OF_SERVICE': 'Mantenimiento'
    };

    const icons = {
        'FREE': '<i class="fa-solid fa-check-circle"></i>',
        'OCCUPIED': '<i class="fa-solid fa-car-side"></i>',
        'OUT_OF_SERVICE': '<i class="fa-solid fa-wrench"></i>'
    };

    div.innerHTML = `
        <div class="spot-icon">${icons[spot.status]}</div>
        <div class="spot-number">${spot.id}</div>
        <div class="spot-status">${statusText[spot.status]}</div>
    `;

    return div;
}

// Actualizar una plaza especifica en el grid
function updateSpotInGrid(spotId, newStatus) {
    const spotElement = document.querySelector(`[data-spot-id="${spotId}"]`);
    if (spotElement) {
        spotElement.classList.remove('FREE', 'OCCUPIED', 'OUT_OF_SERVICE');
        spotElement.classList.add(newStatus);

        const statusText = {
            'FREE': 'Libre',
            'OCCUPIED': 'Ocupada',
            'OUT_OF_SERVICE': 'Mantenimiento'
        };

        const icons = {
            'FREE': '<i class="fa-solid fa-check-circle"></i>',
            'OCCUPIED': '<i class="fa-solid fa-car-side"></i>',
            'OUT_OF_SERVICE': '<i class="fa-solid fa-wrench"></i>'
        };

        spotElement.querySelector('.spot-status').textContent = statusText[newStatus];
        spotElement.querySelector('.spot-icon').innerHTML = icons[newStatus];

        // Show toast
        const toastType = newStatus === 'FREE' ? 'success' : (newStatus === 'OCCUPIED' ? 'warning' : 'error');
        const toastMsg = newStatus === 'FREE' ? 'ahora está libre' : (newStatus === 'OCCUPIED' ? 'ha sido ocupada' : 'está en mantenimiento');
        showToast(`Plaza ${spotId}`, `La plaza ${spotId} ${toastMsg}`, toastType);

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
        'OUT_OF_SERVICE': 'Mantenimiento'
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
            console.log('Estado actualizado:', result);

            // No actualizamos la UI manualmente aqui para evitar duplicados.
            // Esperamos a que el WebSocket nos notifique el cambio (Patron Observer).
            
            // updateSpotInGrid(spotId, status); 
            
            // const statusText = {
            //     'FREE': 'Libre',
            //     'OCCUPIED': 'Ocupada',
            //     'OUT_OF_SERVICE': 'Mantenimiento'
            // };

            // addLogEntry(`Plaza ${spotId} cambiada a ${statusText[status]}`, status);
            loadStatistics();
            loadEvents();
        } else {
            console.error('Error al actualizar estado');
            alert('Error al actualizar el estado de la plaza');
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error de conexion con el servidor');
    }
}

// Conectar WebSocket
function connectWebSocket() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);

    stompClient.connect({}, () => {
        console.log('WebSocket conectado');
        updateConnectionStatus(true);
        showToast('Conectado', 'Conexión en tiempo real establecida', 'success');

        stompClient.subscribe('/topic/parking-updates', (message) => {
            const update = JSON.parse(message.body);
            handleParkingUpdate(update);
        });
    }, (error) => {
        console.error('Error de WebSocket:', error);
        updateConnectionStatus(false);
        showToast('Desconectado', 'Se perdió la conexión con el servidor', 'error');
        setTimeout(connectWebSocket, 5000);
    });
}

// Manejar actualizaciones del parking via WebSocket
function handleParkingUpdate(update) {
    console.log('Actualizacion recibida:', update);
    updateSpotInGrid(update.spotId, update.status);

    const statusText = {
        'FREE': 'Libre',
        'OCCUPIED': 'Ocupada',
        'OUT_OF_SERVICE': 'Mantenimiento'
    };
    addLogEntry(`Plaza ${update.spotId} -> ${statusText[update.status]}`, update.status, update.timestamp);
    loadStatistics();
    loadEvents();
}

// Cargar solo las estadisticas
async function loadStatistics() {
    try {
        const response = await fetch(`${API_URL}/statistics`);
        const stats = await response.json();
        updateStatistics(stats);
    } catch (error) {
        console.error('Error al cargar estadisticas:', error);
    }
}

// Cargar health
async function loadHealth() {
    try {
        const response = await fetch(HEALTH_URL);
        const health = await response.json();
        updateHealthUI(health);
    } catch (error) {
        console.error('No se pudo obtener salud del sistema:', error);
    }
}

function updateHealthUI(health) {
    const status = (health.status || 'DEGRADED').toUpperCase();
    const statusCard = document.getElementById('health-status-card');
    const healthLatency = document.getElementById('health-latency');
    const pill = document.getElementById('health-pill');
    const pillText = document.getElementById('health-status-text');
    const pillUpdated = document.getElementById('health-updated');
    const dot = document.getElementById('health-dot');

    const classes = ['pill-up', 'pill-degraded', 'pill-down'];
    pill.classList.remove(...classes);

    if (status === 'UP') {
        pill.classList.add('pill-up');
        dot.style.backgroundColor = 'var(--status-up)';
    } else if (status === 'DEGRADED') {
        pill.classList.add('pill-degraded');
        dot.style.backgroundColor = 'var(--status-degraded)';
    } else {
        pill.classList.add('pill-down');
        dot.style.backgroundColor = 'var(--status-down)';
    }

    statusCard.textContent = status;
    pillText.textContent = status;

    const feedAge = health.feedAgeMs != null ? `${Math.round(health.feedAgeMs / 1000)}s` : '-';
    const lastFeed = health.lastFeedAt || '-';
    healthLatency.textContent = `Feed: ${feedAge}`;
    pillUpdated.textContent = `Feed: ${lastFeed}`;
}

// Cargar eventos desde API
async function loadEvents() {
    try {
        const response = await fetch(`${EVENTS_URL}?limit=30`);
        const events = await response.json();
        renderEventLog(events);
    } catch (error) {
        console.error('No se pudo obtener eventos:', error);
    }
}

function renderEventLog(events) {
    const log = document.getElementById('activity-log');
    log.innerHTML = '';
    for (let i = events.length - 1; i >= 0; i--) {
        const ev = events[i];
        const msg = ev.message || `Plaza ${ev.spotId} -> ${ev.status}`;
        addLogEntry(msg, ev.status, ev.occurredAt, log);
    }
}

// Actualizar estado de conexion
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

// Anadir entrada al log de actividad
function addLogEntry(message, status, timestamp, targetLog) {
    const log = targetLog || document.getElementById('activity-log');
    const entry = document.createElement('div');
    entry.className = `log-entry ${status || ''}`;

    const eventDate = timestamp ? new Date(timestamp) : new Date();
    const timeString = eventDate.toLocaleTimeString('es-ES');

    const icons = {
        'FREE': '<i class="fa-solid fa-check"></i>',
        'OCCUPIED': '<i class="fa-solid fa-car"></i>',
        'OUT_OF_SERVICE': '<i class="fa-solid fa-wrench"></i>',
        'error': '<i class="fa-solid fa-triangle-exclamation"></i>'
    };

    const icon = icons[status] || '<i class="fa-solid fa-info"></i>';

    entry.innerHTML = `
        <div class="log-icon">${icon}</div>
        <div class="log-content">
            <div class="log-message">${message}</div>
            <div class="log-time">${timeString}</div>
        </div>
    `;

    log.insertBefore(entry, log.firstChild);

    while (log.children.length > 50) {
        log.removeChild(log.lastChild);
    }
}

// Cotizador de tarifas
async function calculateQuote() {
    const minutes = parseInt(document.getElementById('pricing-minutes').value, 10);
    const subscriber = document.getElementById('pricing-subscriber').checked;
    const electricVehicle = document.getElementById('pricing-ev').checked;
    const startInput = document.getElementById('pricing-start').value;

    if (!minutes || minutes <= 0) {
        alert('Introduce minutos validos');
        return;
    }

    const payload = {
        minutes,
        subscriber,
        electricVehicle,
        startTime: startInput ? `${startInput}:00` : null
    };

    try {
        const minutes = document.getElementById('pricing-minutes').value;
        const subscriber = document.getElementById('pricing-subscriber').checked;
        const electric = document.getElementById('pricing-ev').checked;

        const url = `${PRICING_URL}?minutes=${minutes}&subscriber=${subscriber}&electric=${electric}`;

        const response = await fetch(url, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (!response.ok) {
            const err = await response.json();
            renderQuoteError(err.error || 'No se pudo calcular');
            return;
        }

        const quote = await response.json();
        renderQuote(quote);
    } catch (error) {
        console.error('Error al cotizar:', error);
        renderQuoteError('Error de conexion');
    }
}

function renderQuote(quote) {
    const container = document.getElementById('pricing-result');
    container.innerHTML = `
        <div class="quote-grid">
            <span class="label">Base</span><span class="value">${quote.baseAmount} ${quote.currency}</span>
            <span class="label">Recargo pico</span><span class="value">${quote.peakSurcharge} ${quote.currency}</span>
            <span class="label">Recargo EV</span><span class="value">${quote.electricSurcharge} ${quote.currency}</span>
            <span class="label">Descuento</span><span class="value">- ${quote.discount} ${quote.currency}</span>
            <span class="label">Impuestos</span><span class="value">${quote.tax} ${quote.currency}</span>
        </div>
        <div class="quote-total">
            <span>Total</span>
            <span>${quote.total} ${quote.currency}</span>
        </div>
        <p class="muted">Pico: ${quote.peakApplied ? 'Si' : 'No'}</p>
    `;
}

function renderQuoteError(message) {
    const container = document.getElementById('pricing-result');
    container.innerHTML = `<p class="muted">${message}</p>`;
}

// Toast Notifications
function showToast(title, message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    const icons = {
        'success': 'fa-circle-check',
        'error': 'fa-circle-xmark',
        'warning': 'fa-triangle-exclamation',
        'info': 'fa-circle-info'
    };

    toast.innerHTML = `
        <i class="fa-solid ${icons[type]} toast-icon"></i>
        <div class="toast-content">
            <div class="toast-title">${title}</div>
            <div class="toast-message">${message}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()"><i class="fa-solid fa-xmark"></i></button>
    `;

    container.appendChild(toast);

    // Auto remove after 5 seconds
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.3s ease-out forwards';
        setTimeout(() => toast.remove(), 300);
    }, 5000);
}
