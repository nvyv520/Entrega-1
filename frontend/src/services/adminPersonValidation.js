export function normalizeRun(value) {
    if (/\s/u.test(value) || !/^(?:[1-9]\d{0,7}-?[\dkK]|[1-9]\d{0,2}(?:\.\d{3}){1,2}-[\dkK])$/.test(value)) {
        throw new Error("Ingresa un RUN con formato válido, sin espacios y terminado en un número o K.");
    }
    const compact = value.replace(/[.-]/g, "").toUpperCase();
    if (!/^[1-9]\d{0,7}[\dK]$/.test(compact)) throw new Error("El RUN no tiene un formato válido.");
    const body = compact.slice(0, -1);
    const digit = compact.slice(-1);
    return `${body.replace(/\B(?=(\d{3})+(?!\d))/g, ".")}-${digit}`;
}

export function normalizePersonName(value) {
    const particles = ["de", "del", "la", "las", "los", "y", "da", "das", "do", "dos", "van", "von"];
    // Preserve mixed/manual capitalization, including McDonald and particles.
    return value.trim().replace(/\s+/gu, " ").replace(/\p{L}+/gu, word =>
        word === word.toLocaleLowerCase("es-CL") && !particles.includes(word)
            ? word.charAt(0).toLocaleUpperCase("es-CL") + word.slice(1)
            : word
    );
}

export function normalizePersonField(field, value) {
    if (field === "rut") return normalizeRun(value);
    if (["firstName", "lastName"].includes(field)) {
        if (value !== value.trim() || !/^\p{L}[\p{L}\p{M}]*(?:[ '’ʼ-]\p{L}[\p{L}\p{M}]*)*$/u.test(value)) {
            throw new Error("El nombre o apellido solo puede contener letras y separadores válidos, sin espacios al principio o final.");
        }
        const name = normalizePersonName(value);
        if (!name) throw new Error("El nombre y apellido son obligatorios.");
        return name;
    }
    const text = field === "email" ? value : value.trim();
    if (!text) throw new Error("El campo es obligatorio.");
    if (field === "email") {
        const parts = text.split("@");
        const labels = parts.length === 2 ? parts[1].split(".") : [];
        const valid = !/\s/u.test(text) && text.length <= 254 && parts.length === 2 && parts[0].length <= 64
            && /^[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+$/.test(parts[0])
            && !parts[0].startsWith(".") && !parts[0].endsWith(".") && !text.includes("..")
            && parts[1].length <= 253 && labels.length >= 2 && /^[A-Za-z]{2,63}$/.test(labels.at(-1))
            && labels.every(label => label.length <= 63 && /^[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?$/.test(label));
        if (!valid) throw new Error("Ingresa un correo válido, por ejemplo nombre@institucion.cl.");
    }
    return text;
}

export function preparePersonFields(form, fields, setFieldErrors) {
    const errors = {};
    const data = {};
    fields.forEach(field => {
        try {
            data[field] = normalizePersonField(field, form[field]);
        } catch (error) {
            errors[field] = error.message;
        }
    });
    setFieldErrors(errors);
    if (Object.keys(errors).length) throw new Error(Object.values(errors)[0]);
    return data;
}
