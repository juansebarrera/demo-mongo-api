const Clientes = (() => {
  let currentPage = 0;
  let pageSize = 10;
  let currentSearch = '';
  let totalPages = 0;
  let totalElements = 0;
  let perfilesCache = [];

  async function loadPerfiles() {
    try {
      perfilesCache = await API.get('/perfil-riesgo/activos');
    } catch (err) {
      console.error('Error cargando perfiles de riesgo:', err);
      perfilesCache = [];
    }
  }

  function populatePerfilSelect(selectedId) {
    const select = document.getElementById('cPerfilRiesgo');
    select.innerHTML = '<option value="">Sin perfil</option>';
    perfilesCache.forEach(p => {
      const option = document.createElement('option');
      option.value = p.id;
      option.textContent = p.nombre;
      if (p.id === selectedId) option.selected = true;
      select.appendChild(option);
    });
  }

  async function load(page = 0) {
    currentPage = page;
    try {
      if (perfilesCache.length === 0) {
        await loadPerfiles();
      }
      const params = new URLSearchParams({
        page: currentPage,
        size: pageSize,
        sort: 'nombre',
        direction: 'asc',
      });
      if (currentSearch) params.set('search', currentSearch);

      const data = await API.get(`/clientes?${params}`);
      totalPages = data.totalPages;
      totalElements = data.totalElements;
      render(data.content);
      renderPagination();
    } catch (err) {
      console.error('Error cargando clientes:', err);
    }
  }

  function search(term) {
    currentSearch = term.trim();
    load(0);
  }

  function render(items) {
    const tbody = document.getElementById('clientesTable');
    if (!items.length) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No hay clientes registrados</td></tr>';
      return;
    }
    tbody.innerHTML = items.map(c => `
      <tr data-perfil-id="${c.perfilRiesgo ? c.perfilRiesgo.perfilRiesgoId || '' : ''}">
        <td>${esc(c.nombre)}</td>
        <td>${esc(c.email || '-')}</td>
        <td>${esc(c.telefono || '-')}</td>
        <td>${esc(c.direccion || '-')}</td>
        <td>${esc(c.perfilRiesgo ? c.perfilRiesgo.perfilDescripcion || '-' : '-')}</td>
        <td class="actions">
          <button class="btn btn-sm btn-ghost" onclick="Clientes.edit('${c.id}')">Editar</button>
          ${API.isAdmin() ? `<button class="btn btn-sm btn-danger" onclick="Clientes.remove('${c.id}')">Eliminar</button>` : ''}
        </td>
      </tr>
    `).join('');
  }

  function renderPagination() {
    const container = document.getElementById('paginationClientes');
    if (!container || totalPages <= 1) {
      if (container) container.innerHTML = '';
      return;
    }

    let html = `<span class="pagination-info">${totalElements} resultado${totalElements !== 1 ? 's' : ''}</span><div class="pagination-buttons">`;
    html += `<button class="btn btn-sm btn-ghost" ${currentPage === 0 ? 'disabled' : ''} onclick="Clientes.load(${currentPage - 1})">Anterior</button>`;

    const maxVisible = 5;
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible);
    if (end - start < maxVisible) start = Math.max(0, end - maxVisible);

    for (let i = start; i < end; i++) {
      html += `<button class="btn btn-sm ${i === currentPage ? 'btn-primary' : 'btn-ghost'}" onclick="Clientes.load(${i})">${i + 1}</button>`;
    }

    html += `<button class="btn btn-sm btn-ghost" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="Clientes.load(${currentPage + 1})">Siguiente</button>`;
    html += '</div>';
    container.innerHTML = html;
  }

  async function openCreate() {
    await loadPerfiles();
    document.getElementById('modalTitleC').textContent = 'Nuevo Cliente';
    document.getElementById('formIdC').value = '';
    document.getElementById('cNombre').value = '';
    document.getElementById('cEmail').value = '';
    document.getElementById('cTelefono').value = '';
    document.getElementById('cDireccion').value = '';
    populatePerfilSelect('');
    document.getElementById('modalOverlayC').classList.add('active');
  }

  async function edit(id) {
    await loadPerfiles();
    const tbody = document.getElementById('clientesTable');
    const rows = tbody.querySelectorAll('tr');
    let item = null;
    for (const row of rows) {
      const editBtn = row.querySelector(`[onclick*="${id}"]`);
      if (editBtn) {
        item = {
          id,
          nombre: row.cells[0]?.textContent,
          email: row.cells[1]?.textContent === '-' ? '' : row.cells[1]?.textContent,
          telefono: row.cells[2]?.textContent === '-' ? '' : row.cells[2]?.textContent,
          direccion: row.cells[3]?.textContent === '-' ? '' : row.cells[3]?.textContent,
          perfilRiesgoId: row.dataset.perfilId || '',
        };
        break;
      }
    }
    if (!item) return;

    document.getElementById('modalTitleC').textContent = 'Editar Cliente';
    document.getElementById('formIdC').value = item.id;
    document.getElementById('cNombre').value = item.nombre;
    document.getElementById('cEmail').value = item.email || '';
    document.getElementById('cTelefono').value = item.telefono || '';
    document.getElementById('cDireccion').value = item.direccion || '';
    populatePerfilSelect(item.perfilRiesgoId);
    document.getElementById('modalOverlayC').classList.add('active');
  }

  async function save() {
    const id = document.getElementById('formIdC').value;
    const perfilId = document.getElementById('cPerfilRiesgo').value;
    const body = {
      nombre: document.getElementById('cNombre').value.trim(),
      email: document.getElementById('cEmail').value.trim(),
      telefono: document.getElementById('cTelefono').value.trim(),
      direccion: document.getElementById('cDireccion').value.trim(),
      perfilRiesgoId: perfilId || null,
    };

    if (!body.nombre) {
      Toast.show('El nombre es obligatorio');
      return;
    }

    try {
      if (id) {
        await API.put(`/clientes/${id}`, body);
      } else {
        await API.post('/clientes', body);
      }
      closeModal();
      await load(currentPage);
    } catch (err) {
      Toast.show(err.message);
    }
  }

  async function remove(id) {
    if (!confirm('Eliminar este cliente?')) return;
    try {
      await API.del(`/clientes/${id}`);
      await load(currentPage);
    } catch (err) {
      Toast.show(err.message);
    }
  }

  function closeModal() {
    document.getElementById('modalOverlayC').classList.remove('active');
  }

  async function exportCsv() {
    try {
      await API.download('/clientes/export', 'clientes.csv');
    } catch (err) {
      Toast.show(err.message);
    }
  }

  function esc(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  return { load, search, openCreate, edit, save, remove, closeModal, exportCsv };
})();
