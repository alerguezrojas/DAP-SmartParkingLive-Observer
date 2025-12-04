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
    loadParkingList();
    loadHistory();
    connectWebSocket();
    loadHealth();
    loadEvents();
    loadSidebarUsers();
    setInterval(loadHealth, 30000);
    setInterval(loadEvents, 20000);
    setInterval(loadSidebarUsers, 30000); // Refresh user list periodically
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
                    tension: 0,
                    stepped: true,
                    pointRadius: 0,
                    pointHoverRadius: 6
                },
                {
                    label: 'Libres',
                    data: [],
                    borderColor: '#10b981',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    fill: true,
                    tension: 0,
                    stepped: true,
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

    let chartData = [...history];

    // 1. Si solo hay un registro, crear un punto anterior para tener una línea
    if (chartData.length === 1) {
        const point = chartData[0];
        const currentTimestamp = new Date(point.timestamp).getTime();
        const prevTimestamp = new Date(currentTimestamp - 10 * 60 * 1000).toISOString();
        
        chartData.unshift({
            ...point,
            timestamp: prevTimestamp
        });
    }

    // 2. Proyectar hasta "AHORA" para evitar que la gráfica parezca cortada
    if (chartData.length > 0) {
        const lastPoint = chartData[chartData.length - 1];
        const lastTime = new Date(lastPoint.timestamp).getTime();
        const now = Date.now();

        // Si el último dato tiene más de 1 minuto de antigüedad, añadimos un punto "ahora"
        if (now - lastTime > 60000) {
            chartData.push({
                ...lastPoint,
                timestamp: new Date(now).toISOString()
            });
        }
    }

    // Mapear a formato {x, y} para escala de tiempo
    const occupiedData = chartData.map(h => ({x: h.timestamp, y: h.occupied}));
    const freeData = chartData.map(h => ({x: h.timestamp, y: h.free}));

    historyChart.data.datasets[0].data = occupiedData;
    historyChart.data.datasets[1].data = freeData;
    historyChart.update();
}

// Cargar datos del parking
function loadParkingList() {
    fetch(`${API_URL}/list`)
        .then(response => response.json())
        .then(data => {
            const selector = document.getElementById('parking-selector');
            selector.innerHTML = '<option value="" disabled>Seleccionar Parking</option>';
            
            // Sort by ID
            data.sort((a, b) => a.id.localeCompare(b.id));

            data.forEach(p => {
                const option = document.createElement('option');
                option.value = p.id;
                option.textContent = `${p.id} (Libres: ${p.availableLots})`;
                selector.appendChild(option);
            });

            // Set current active parking
            fetch(`${API_URL}/feed`)
                .then(res => res.json())
                .then(feed => {
                    if (feed && feed.carparkNumber) {
                        selector.value = feed.carparkNumber;
                    }
                })
                .catch(e => console.log("No active feed yet"));

            // Remove old listeners to avoid duplicates if called multiple times
            const newSelector = selector.cloneNode(true);
            selector.parentNode.replaceChild(newSelector, selector);
            
            newSelector.addEventListener('change', (e) => {
                const selectedId = e.target.value;
                selectParking(selectedId);
            });
        })
        .catch(err => console.error('Error cargando lista de parkings:', err));
}

function selectParking(id) {
    fetch(`${API_URL}/select/${id}`, { method: 'POST' })
        .then(response => {
            if (response.ok) {
                showToast(`Parking ${id} seleccionado`, 'success');
                // Reload everything
                loadParkingData();
                loadHistory(); 
                loadHealth();
                loadEvents();
            } else {
                showToast('Error seleccionando parking', 'error');
            }
        })
        .catch(err => showToast('Error de conexión', 'error'));
}

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

    // Si el numero total de plazas ha cambiado, recargar el grid
    if (spots && spots.length !== stats.total) {
        console.log(`Detectado cambio en capacidad: ${spots.length} -> ${stats.total}. Recargando grid...`);
        loadParkingData();
    }

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

        // KDD Notifications
        if (currentKddUser) {
            subscribeToKddNotifications();
        }

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

// --- KDD Logic ---

let currentKddUser = null;

function registerKddUser() {
    const name = document.getElementById('kdd-username').value;
    const lat = document.getElementById('kdd-lat').value;
    const lon = document.getElementById('kdd-lon').value;
    const isMod = document.getElementById('kdd-ismod').checked;

    if (!name) {
        showToast('Error', 'El nombre es obligatorio', 'error');
        return;
    }

    if (name.includes(' ')) {
        showToast('Error', 'El nombre debe ser una única palabra (sin espacios)', 'error');
        return;
    }

    fetch('/api/kdd/users', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, lat, lon, isMod })
    })
    .then(res => res.json())
    .then(user => {
        currentKddUser = user;
        document.getElementById('kdd-user-status').innerHTML = `Registrado como: <strong>${user.name}</strong> (ID: ${user.id.substring(0,8)}...)`;
        showToast('KDD', 'Usuario registrado correctamente', 'success');
        loadKddEvents();
        loadSidebarUsers();
        
        // Subscribe to notifications if WS is connected
        if (stompClient && stompClient.connected) {
            subscribeToKddNotifications();
        }
    })
    .catch(err => showToast('Error', 'Fallo al registrar usuario', 'error'));
}

function createKddEvent() {
    if (!currentKddUser) {
        showToast('Error', 'Debes registrarte primero', 'error');
        return;
    }

    const name = document.getElementById('event-name').value;
    const description = document.getElementById('event-desc').value;
    const lat = document.getElementById('event-lat').value;
    const lon = document.getElementById('event-lon').value;

    if (!name || !lat || !lon) {
        showToast('Error', 'Completa los campos obligatorios', 'error');
        return;
    }

    fetch('/api/kdd/events', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            name, description, lat, lon,
            creatorId: currentKddUser.id
        })
    })
    .then(res => res.json())
    .then(event => {
        showToast('KDD', 'Evento creado exitosamente', 'success');
        loadKddEvents();
        // Clear form
        document.getElementById('event-name').value = '';
        document.getElementById('event-desc').value = '';
    })
    .catch(err => showToast('Error', 'Fallo al crear evento', 'error'));
}

function loadKddEvents() {
    fetch('/api/kdd/events')
        .then(res => res.json())
        .then(events => {
            const container = document.getElementById('kdd-events-list');
            container.innerHTML = '';
            events.forEach(event => {
                const participantsCount = event.participants ? event.participants.length : 0;
                const isJoined = currentKddUser && event.participants && event.participants.includes(currentKddUser.name);
                
                const div = document.createElement('div');
                div.className = 'kdd-event-card';
                div.style.cssText = 'padding: 10px; border-bottom: 1px solid #eee; margin-bottom: 5px;';
                div.innerHTML = `
                    <div style="display: flex; justify-content: space-between; align-items: start;">
                        <div>
                            <h4 style="margin: 0 0 5px 0;">${event.name}</h4>
                            <p style="margin: 0; color: #666; font-size: 0.9em;">${event.description || 'Sin descripción'}</p>
                            <small style="color: #999;">
                                Por: ${event.creatorName} | <i class="fa-solid fa-users"></i> ${participantsCount}
                            </small>
                        </div>
                        ${currentKddUser && !isJoined ? 
                            `<button onclick="joinEvent('${event.id}')" style="padding: 4px 8px; font-size: 0.8rem; background: #007AFF; color: white; border: none; border-radius: 4px; cursor: pointer;">Unirse</button>` 
                            : (isJoined ? '<span style="font-size: 0.8rem; color: #10b981;"><i class="fa-solid fa-check"></i> Unido</span>' : '')}
                    </div>
                `;
                container.appendChild(div);
            });
        });
}

function joinEvent(eventId) {
    if (!currentKddUser) return;
    
    fetch(`/api/kdd/events/${eventId}/join`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: currentKddUser.id })
    })
    .then(res => {
        if (res.ok) {
            showToast('KDD', 'Te has unido al evento', 'success');
            loadKddEvents();
        } else {
            showToast('Error', 'No se pudo unir al evento', 'error');
        }
    });
}

function subscribeToKddNotifications() {
    // In a real app with auth, we would subscribe to /user/topic/kdd/notifications
    // Here we subscribe to the public topic and filter by ID (client-side filtering for demo)
    // BUT, the backend sends to /topic/kdd/notifications with targetUserId inside.
    stompClient.subscribe('/topic/kdd/notifications', (message) => {
        const notification = JSON.parse(message.body);
        if (currentKddUser && notification.targetUserId === currentKddUser.id) {
            showToast('Nueva KDD Cerca!', notification.message, 'info');
            loadKddEvents();
        }
    });
}

// Navigation Logic
document.querySelectorAll('.sidebar-nav li').forEach(li => {
    li.addEventListener('click', (e) => {
        e.preventDefault();
        // Remove active class
        document.querySelectorAll('.sidebar-nav li').forEach(l => l.classList.remove('active'));
        li.classList.add('active');

        // Hide all views
        document.querySelectorAll('.view-section').forEach(v => v.style.display = 'none');
        
        // Show target view
        const targetId = li.getAttribute('data-target');
        if (targetId) {
            const targetEl = document.getElementById(targetId);
            if (targetEl) {
                targetEl.style.display = ''; // Revert to CSS defined display (flex)
            }
        }
    });
});

function loadSidebarUsers() {
    fetch('/api/kdd/users')
        .then(res => res.json())
        .then(users => {
            const container = document.getElementById('sidebar-user-list');
            if (!container) return;
            
            container.innerHTML = '';
            users.forEach(user => {
                const li = document.createElement('li');
                li.style.padding = '8px 20px';
                li.style.fontSize = '0.9rem';
                
                const a = document.createElement('a');
                a.href = `/usuario/${user.name}`;
                a.target = '_blank'; // Open in new tab or remove to navigate
                a.style.color = 'rgba(255,255,255,0.7)';
                a.style.textDecoration = 'none';
                a.style.display = 'flex';
                a.style.alignItems = 'center';
                a.style.gap = '10px';
                a.innerHTML = `<i class="fa-solid fa-user-circle"></i> ${user.name}`;
                
                // Hover effect
                a.onmouseover = () => a.style.color = '#fff';
                a.onmouseout = () => a.style.color = 'rgba(255,255,255,0.7)';

                li.appendChild(a);
                container.appendChild(li);
            });
        })
        .catch(err => console.error('Error loading users:', err));
}

