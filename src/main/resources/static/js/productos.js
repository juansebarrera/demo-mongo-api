const Productos = (() => {
  let currentPage = 0;
  let pageSize = 10;
  let currentSearch = '';
  let totalPages = 0;
  let totalElements = 0;

  async function load(page = 0) {
    currentPage = page;
    try {
      const params = new URLSearchParams({
        page: currentPage,
        size: pageSize,
        sort: 'nombre',
        direction: 'asc',
      });
      if (currentSearch) params.set('search', currentSearch);

      const data = await API.get(`/productos?${params}`);
      totalPages = data.totalPages;
      totalElements = data.totalElements;
      render(data.content);
      renderPagination();
    } catch (err) {
      console.error('Error cargando productos:', err);
    }
  }

  function search(term) {
    currentSearch = term.trim();
    load(0);
  }

  function render(items) {
    const tbody = document.getElementById('productosTable');
    if (!items.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No hay productos registrados</td></tr>';
      return;
    }
    tbody.innerHTML = items.map(p => `
      <tr>
        <td>${esc(p.nombre)}</td>
        <td>${esc(p.descripcion || '-')}</td>
        <td>$${Number(p.precio).toFixed(2)}</td>
        <td>${p.stock}</td>
        <td class="actions">
          <button class="btn btn-sm btn-ghost" onclick="Productos.edit('${p.id}')">Editar</button>
          ${API.isAdmin() ? `<button class="btn btn-sm btn-danger" onclick="Productos.remove('${p.id}')">Eliminar</button>` : ''}
        </td>
      </tr>
    `).join('');
  }

  function renderPagination() {
    const container = document.getElementById('paginationProductos');
    if (!container || totalPages <= 1) {
      if (container) container.innerHTML = '';
      return;
    }

    let html = `<span class="pagination-info">${totalElements} resultado${totalElements !== 1 ? 's' : ''}</span><div class="pagination-buttons">`;
    html += `<button class="btn btn-sm btn-ghost" ${currentPage === 0 ? 'disabled' : ''} onclick="Productos.load(${currentPage - 1})">Anterior</button>`;

    const maxVisible = 5;
    let start = Math.max(0, currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible);
    if (end - start < maxVisible) start = Math.max(0, end - maxVisible);

    for (let i = start; i < end; i++) {
      html += `<button class="btn btn-sm ${i === currentPage ? 'btn-primary' : 'btn-ghost'}" onclick="Productos.load(${i})">${i + 1}</button>`;
    }

    html += `<button class="btn btn-sm btn-ghost" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="Productos.load(${currentPage + 1})">Siguiente</button>`;
    html += '</div>';
    container.innerHTML = html;
  }

  function openCreate() {
    document.getElementById('modalTitle').textContent = 'Nuevo Producto';
    document.getElementById('formId').value = '';
    document.getElementById('pNombre').value = '';
    document.getElementById('pDescripcion').value = '';
    document.getElementById('pPrecio').value = '';
    document.getElementById('pStock').value = '';
    document.getElementById('modalOverlay').classList.add('active');
  }

  function edit(id) {
    const item = findItem(id);
    if (!item) return;
    document.getElementById('modalTitle').textContent = 'Editar Producto';
    document.getElementById('formId').value = item.id;
    document.getElementById('pNombre').value = item.nombre;
    document.getElementById('pDescripcion').value = item.descripcion || '';
    document.getElementById('pPrecio').value = item.precio;
    document.getElementById('pStock').value = item.stock;
    document.getElementById('modalOverlay').classList.add('active');
  }

  function findItem(id) {
    const tbody = document.getElementById('productosTable');
    const rows = tbody.querySelectorAll('tr');
    for (const row of rows) {
      const editBtn = row.querySelector(`[onclick*="${id}"]`);
      if (editBtn) {
        return {
          id,
          nombre: row.cells[0]?.textContent,
          descripcion: row.cells[1]?.textContent === '-' ? '' : row.cells[1]?.textContent,
          precio: parseFloat(row.cells[2]?.textContent.replace('$', '')),
          stock: parseInt(row.cells[3]?.textContent),
        };
      }
    }
    return null;
  }

  async function save() {
    const id = document.getElementById('formId').value;
    const body = {
      nombre: document.getElementById('pNombre').value.trim(),
      descripcion: document.getElementById('pDescripcion').value.trim(),
      precio: parseFloat(document.getElementById('pPrecio').value),
      stock: parseInt(document.getElementById('pStock').value, 10),
    };

    if (!body.nombre || isNaN(body.precio) || isNaN(body.stock)) {
      alert('Completa todos los campos correctamente');
      return;
    }

    try {
      if (id) {
        await API.put(`/productos/${id}`, body);
      } else {
        await API.post('/productos', body);
      }
      closeModal();
      await load(currentPage);
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  async function remove(id) {
    if (!confirm('Eliminar este producto?')) return;
    try {
      await API.del(`/productos/${id}`);
      await load(currentPage);
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  function closeModal() {
    document.getElementById('modalOverlay').classList.remove('active');
  }

  async function exportCsv() {
    try {
      await API.download('/productos/export', 'productos.csv');
    } catch (err) {
      alert('Error al exportar: ' + err.message);
    }
  }

  function esc(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  return { load, search, openCreate, edit, save, remove, closeModal, exportCsv };
})();
