-- Tabla de relación many-to-many entre tasks y staff para permitir múltiples asignaciones
CREATE TABLE IF NOT EXISTS task_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    assigned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uix_task_staff (task_id, staff_id),
    CONSTRAINT fk_task_assignments_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignments_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);

-- Migrar datos existentes de assignee_id a task_assignments si existen
-- Nota: assignee_id en tasks referencia users(id), pero task_assignments referencia staff(id)
-- Solo migraremos si el assignee_id corresponde a un usuario que tiene un registro en staff
-- Esto requiere que primero se hayan creado los registros de staff para los usuarios correspondientes
INSERT INTO task_assignments (task_id, staff_id, assigned_at)
SELECT t.id, s.id, t.created_at
FROM tasks t
INNER JOIN users u ON u.id = t.assignee_id
INNER JOIN staff s ON s.building_id = t.building_id 
    AND (s.email = u.email OR s.rut = u.document_number)
WHERE t.assignee_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM task_assignments ta 
    WHERE ta.task_id = t.id AND ta.staff_id = s.id
);
