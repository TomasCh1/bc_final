
/**
 * Show password change modal
 */
function showPasswordChangeModal(userId, isFirstLogin = false) {
    const modal = document.createElement('div');
    modal.id = 'passwordChangeModal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        justify-content: center;
        align-items: center;
        z-index: 1000;
    `;

    modal.innerHTML = `
        <div style="background: var(--bg-primary); padding: 2rem; border-radius: 8px; max-width: 400px; width: 90%;">
            <h2 style="margin-bottom: 1.5rem;">${isFirstLogin ? 'Change Password (Required)' : 'Change Password'}</h2>
            ${isFirstLogin ? '<p style="color: var(--text-secondary); margin-bottom: 1rem;">You must change your password before continuing.</p>' : ''}
            <form id="passwordChangeForm">
                ${!isFirstLogin ? `
                    <div style="margin-bottom: 1rem;">
                        <label style="display: block; margin-bottom: 0.5rem; font-weight: 500;">Current Password</label>
                        <input type="password" id="currentPassword" required style="width: 100%; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: 4px;">
                    </div>
                ` : ''}
                <div style="margin-bottom: 1rem;">
                    <label style="display: block; margin-bottom: 0.5rem; font-weight: 500;">New Password</label>
                    <input type="password" id="newPassword" required minlength="6" style="width: 100%; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: 4px;">
                </div>
                <div style="margin-bottom: 1.5rem;">
                    <label style="display: block; margin-bottom: 0.5rem; font-weight: 500;">Confirm New Password</label>
                    <input type="password" id="confirmPassword" required minlength="6" style="width: 100%; padding: 0.75rem; border: 1px solid var(--border-color); border-radius: 4px;">
                </div>
                <div id="passwordError" style="color: #dc3545; margin-bottom: 1rem; display: none;"></div>
                <div style="display: flex; gap: 0.5rem; justify-content: flex-end;">
                    ${isFirstLogin ? '' : '<button type="button" onclick="closePasswordChangeModal()" style="padding: 0.75rem 1.5rem; background: var(--text-secondary); color: white; border: none; border-radius: 4px; cursor: pointer;">Cancel</button>'}
                    <button type="submit" style="padding: 0.75rem 1.5rem; background: var(--accent-color); color: white; border: none; border-radius: 4px; cursor: pointer;">Change Password</button>
                </div>
            </form>
        </div>
    `;

    document.body.appendChild(modal);

    document.getElementById('passwordChangeForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await handlePasswordChange(userId, isFirstLogin);
    });

    if (!isFirstLogin) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closePasswordChangeModal();
            }
        });
    }
}

/**
 * Close password change modal
 */
function closePasswordChangeModal() {
    const modal = document.getElementById('passwordChangeModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * Handle password change form submission
 */
async function handlePasswordChange(userId, isFirstLogin) {
    const errorDiv = document.getElementById('passwordError');
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const currentPassword = isFirstLogin ? '' : document.getElementById('currentPassword').value;

    if (newPassword !== confirmPassword) {
        errorDiv.textContent = 'New password and confirmation do not match';
        errorDiv.style.display = 'block';
        return;
    }

    if (newPassword.length < 6) {
        errorDiv.textContent = 'Password must be at least 6 characters';
        errorDiv.style.display = 'block';
        return;
    }

    errorDiv.style.display = 'none';

    try {
        const passwordData = {
            currentPassword: currentPassword,
            newPassword: newPassword,
            confirmPassword: confirmPassword
        };

        await changePassword(userId, passwordData);
        
        alert('Password changed successfully!');
        closePasswordChangeModal();
        
        if (isFirstLogin) {
            window.location.reload();
        }
    } catch (error) {
        errorDiv.textContent = error.message || 'Failed to change password';
        errorDiv.style.display = 'block';
    }
}

/**
 * Check if user needs to change password on login
 */
async function checkPasswordChangeRequired() {
    try {
        const user = getCurrentUser();
        if (!user) return;

        const userProfile = await getCurrentUserProfile();
        
        if (userProfile.mustChangePassword) {
            showPasswordChangeModal(userProfile.id, true);
        }
    } catch (error) {
        console.error('Error checking password change requirement:', error);
    }
}

