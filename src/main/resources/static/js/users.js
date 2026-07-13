const Usuarios = (() => {
  let usuarios = [];

  async function load() {
    try {
      usuarios = await API.get('/usuarios');
      render();
    } catch (err) {
      console.error('Error cargando usuarios:', err);
    }
  }

  function render() {
    const tbody = document.getElementById('usuariosTable');
    if (!usuarios.length) {
      tbody.innerHTML = '<tr><td colspan="4" class="empty-state">No hay usuarios registrados</td></tr>';
      return;
    }
    tbody.innerHTML = usuarios.map(u => `
      <tr>
        <td>${esc(u.username)}</td>
        <td>${u.roles.map(r => r.replace('ROLE_', '')).join(', ')}</td>
        <td><span class="badge ${u.enabled ? 'badge-success' : 'badge-danger'}">${u.enabled ? 'Activo' : 'Inactivo'}</span></td>
        <td class="actions">
          <button class="btn btn-sm btn-ghost" onclick="Usuarios.edit('${u.id}')">Editar</button>
          <button class="btn btn-sm btn-ghost" onclick="Usuarios.changePassword('${u.id}')">Contraseña</button>
          <button class="btn btn-sm ${u.enabled ? 'btn-warning' : 'btn-success'}" onclick="Usuarios.toggleActive('${u.id}')">${u.enabled ? 'Desactivar' : 'Activar'}</button>
          <button class="btn btn-sm btn-danger" onclick="Usuarios.remove('${u.id}')">Eliminar</button>
        </td>
      </tr>
    `).join('');
  }

  function openCreate() {
    document.getElementById('modalTitleU').textContent = 'Nuevo Usuario';
    document.getElementById('formIdU').value = '';
    document.getElementById('uUsername').value = '';
    document.getElementById('uPassword').value = '';
    document.getElementById('uPasswordGroup').style.display = '';
    document.getElementById('uRoles').value = 'ROLE_USER';
    document.getElementById('modalOverlayU').classList.add('active');
  }

  function edit(id) {
    const u = usuarios.find(x => x.id === id);
    if (!u) return;
    document.getElementById('modalTitleU').textContent = 'Editar Usuario';
    document.getElementById('formIdU').value = u.id;
    document.getElementById('uUsername').value = u.username;
    document.getElementById('uPasswordGroup').style.display = 'none';
    document.getElementById('uRoles').value = u.roles.includes('ROLE_ADMIN') ? 'ROLE_ADMIN' : 'ROLE_USER';
    document.getElementById('modalOverlayU').classList.add('active');
  }

  async function save() {
    const id = document.getElementById('formIdU').value;
    const username = document.getElementById('uUsername').value.trim();
    const roles = [document.getElementById('uRoles').value];

    if (!username) {
      alert('El nombre de usuario es obligatorio');
      return;
    }

    try {
      if (id) {
        await API.put(`/usuarios/${id}`, { username, roles });
      } else {
        const password = document.getElementById('uPassword').value;
        if (!password) {
          alert('La contraseña es obligatoria');
          return;
        }
        await API.post('/usuarios', { username, password, roles });
      }
      closeModal();
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  async function remove(id) {
    if (!confirm('¿Eliminar este usuario?')) return;
    try {
      await API.del(`/usuarios/${id}`);
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  async function toggleActive(id) {
    try {
      await API.put(`/usuarios/${id}/toggle-active`);
      await load();
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  function changePassword(id) {
    document.getElementById('formIdP').value = id;
    document.getElementById('uNewPassword').value = '';
    document.getElementById('modalOverlayP').classList.add('active');
  }

  async function savePassword() {
    const id = document.getElementById('formIdP').value;
    const newPassword = document.getElementById('uNewPassword').value;
    if (!newPassword) {
      alert('La contraseña es obligatoria');
      return;
    }
    try {
      await API.put(`/usuarios/${id}/password`, { currentPassword: '', newPassword });
      closeModalP();
      alert('Contraseña actualizada correctamente');
    } catch (err) {
      alert('Error: ' + err.message);
    }
  }

  function closeModal() {
    document.getElementById('modalOverlayU').classList.remove('active');
  }

  function closeModalP() {
    document.getElementById('modalOverlayP').classList.remove('active');
  }

  function esc(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  return { load, openCreate, edit, save, remove, toggleActive, changePassword, savePassword, closeModal, closeModalP };
})();
