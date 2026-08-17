export default function AvatarCircle({ photoUrl, initials, className = '' }) {
  if (photoUrl) {
    return <img src={photoUrl} alt="" className={'avatar-circle avatar-photo ' + className} />;
  }
  return <div className={'avatar-circle ' + className}>{initials}</div>;
}
