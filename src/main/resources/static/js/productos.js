const Productos = (() => {
  let items = [];

  async function load() {
    try {
      items = await API.get('/productos');
      render();
    } catch (err) {
      console.error('Error cargando productos:', err);
    }
  }

  function render() {
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
    const item = items.find(p => p.id === id);
    if (!item) return;
    document.getElementById('modalTitle').textContent = 'Editar Producto';
    document.getElementById('formId').value = item.id;
    document.getElementById('pNombre').value = item.nombre;
    document.getElementById('pDescripcion').value = item.descripcion || '';
    document.getElementById('pPrecio').value = item.precio;
    document.getElementById('pStock').value = item.stock;
    document.getElementById('modalOverlay').classList.add('active');
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
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  async function remove(id) {
    if (!confirm('Eliminar este producto?')) return;
    try {
      await API.del(`/productos/${id}`);
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  function closeModal() {
    document.getElementById('modalOverlay').classList.remove('active');
  }

  function esc(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  return { load, openCreate, edit, save, remove, closeModal };
})();
