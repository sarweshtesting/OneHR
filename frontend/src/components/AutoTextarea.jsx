import { useEffect, useRef } from 'react';

/** A textarea that grows to fit its content instead of scrolling internally. */
export default function AutoTextarea({ value, className, ...rest }) {
  const ref = useRef(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = el.scrollHeight + 'px';
  }, [value]);

  return <textarea ref={ref} value={value} className={'auto-resize' + (className ? ' ' + className : '')} {...rest} />;
}
