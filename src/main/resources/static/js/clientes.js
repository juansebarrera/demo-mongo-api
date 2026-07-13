const Clientes = (() => {
  let items = [];

  async function load() {
    try {
      items = await API.get('/clientes');
      render();
    } catch (err) {
      console.error('Error cargando clientes:', err);
    }
  }

  function render() {
    const tbody = document.getElementById('clientesTable');
    if (!items.length) {
      tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No hay clientes registrados</td></tr>';
      return;
    }
    tbody.innerHTML = items.map(c => `
      <tr>
        <td>${esc(c.nombre)}</td>
        <td>${esc(c.email || '-')}</td>
        <td>${esc(c.telefono || '-')}</td>
        <td>${esc(c.direccion || '-')}</td>
        <td class="actions">
          <button class="btn btn-sm btn-ghost" onclick="Clientes.edit('${c.id}')">Editar</button>
          ${API.isAdmin() ? `<button class="btn btn-sm btn-danger" onclick="Clientes.remove('${c.id}')">Eliminar</button>` : ''}
        </td>
      </tr>
    `).join('');
  }

  function openCreate() {
    document.getElementById('modalTitleC').textContent = 'Nuevo Cliente';
    document.getElementById('formIdC').value = '';
    document.getElementById('cNombre').value = '';
    document.getElementById('cEmail').value = '';
    document.getElementById('cTelefono').value = '';
    document.getElementById('cDireccion').value = '';
    document.getElementById('modalOverlayC').classList.add('active');
  }

  function edit(id) {
    const item = items.find(c => c.id === id);
    if (!item) return;
    document.getElementById('modalTitleC').textContent = 'Editar Cliente';
    document.getElementById('formIdC').value = item.id;
    document.getElementById('cNombre').value = item.nombre;
    document.getElementById('cEmail').value = item.email || '';
    document.getElementById('cTelefono').value = item.telefono || '';
    document.getElementById('cDireccion').value = item.direccion || '';
    document.getElementById('modalOverlayC').classList.add('active');
  }

  async function save() {
    const id = document.getElementById('formIdC').value;
    const body = {
      nombre: document.getElementById('cNombre').value.trim(),
      email: document.getElementById('cEmail').value.trim(),
      telefono: document.getElementById('cTelefono').value.trim(),
      direccion: document.getElementById('cDireccion').value.trim(),
    };

    if (!body.nombre) {
      alert('El nombre es obligatorio');
      return;
    }

    try {
      if (id) {
        await API.put(`/clientes/${id}`, body);
      } else {
        await API.post('/clientes', body);
      }
      closeModal();
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  async function remove(id) {
    if (!confirm('Eliminar este cliente?')) return;
    try {
      await API.del(`/clientes/${id}`);
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  function closeModal() {
    document.getElementById('modalOverlayC').classList.remove('active');
  }

  function esc(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  return { load, openCreate, edit, save, remove, closeModal };
})();
