const Perfiles = (() => {
  let perfiles = [];

  async function load() {
    try {
      perfiles = await API.get('/perfil-riesgo');
      render();
    } catch (err) {
      console.error('Error cargando perfiles de riesgo:', err);
    }
  }

  function render() {
    const tbody = document.getElementById('perfilesTable');
    if (!perfiles.length) {
      tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No hay perfiles de riesgo registrados</td></tr>';
      return;
    }
    tbody.innerHTML = perfiles.map(p => `
      <tr>
        <td>${esc(p.nombre)}</td>
        <td>${esc(p.descripcion || '-')}</td>
        <td><span class="badge ${p.activo ? 'badge-success' : 'badge-danger'}">${p.activo ? 'Activo' : 'Inactivo'}</span></td>
        <td class="actions">
          <button class="btn btn-sm btn-ghost" onclick="Perfiles.edit('${p.id}')">Editar</button>
          <button class="btn btn-sm ${p.activo ? 'btn-warning' : 'btn-success'}" onclick="Perfiles.toggleActive('${p.id}')">${p.activo ? 'Desactivar' : 'Activar'}</button>
          <button class="btn btn-sm btn-danger" onclick="Perfiles.remove('${p.id}')">Eliminar</button>
        </td>
      </tr>
    `).join('');
  }

  function openCreate() {
    document.getElementById('modalTitlePR').textContent = 'Nuevo Perfil de Riesgo';
    document.getElementById('formIdPR').value = '';
    document.getElementById('prNombre').value = '';
    document.getElementById('prDescripcion').value = '';
    document.getElementById('modalOverlayPR').classList.add('active');
  }

  function edit(id) {
    const p = perfiles.find(x => x.id === id);
    if (!p) return;
    document.getElementById('modalTitlePR').textContent = 'Editar Perfil de Riesgo';
    document.getElementById('formIdPR').value = p.id;
    document.getElementById('prNombre').value = p.nombre;
    document.getElementById('prDescripcion').value = p.descripcion || '';
    document.getElementById('modalOverlayPR').classList.add('active');
  }

  async function save() {
    const id = document.getElementById('formIdPR').value;
    const nombre = document.getElementById('prNombre').value.trim();
    const descripcion = document.getElementById('prDescripcion').value.trim();

    if (!nombre) {
      alert('El nombre es obligatorio');
      return;
    }

    const body = { nombre, descripcion };

    try {
      if (id) {
        const existing = perfiles.find(x => x.id === id);
        body.activo = existing ? existing.activo : true;
        await API.put(`/perfil-riesgo/${id}`, body);
      } else {
        await API.post('/perfil-riesgo', body);
      }
      closeModal();
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  async function remove(id) {
    if (!confirm('¿Eliminar este perfil de riesgo?')) return;
    try {
      await API.del(`/perfil-riesgo/${id}`);
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  async function toggleActive(id) {
    try {
      await API.put(`/perfil-riesgo/${id}/toggle-active`);
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  function closeModal() {
    document.getElementById('modalOverlayPR').classList.remove('active');
  }

  function esc(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  return { load, openCreate, edit, save, remove, toggleActive, closeModal };
})();
