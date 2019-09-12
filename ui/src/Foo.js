import React, {Component} from 'react';
import { withRouter } from 'react-router-dom';

class Foo extends Component {
  render() {
    return (
      <div className="admin">
        Foo
      </div>
    );
  }
}

export default withRouter(Foo);
